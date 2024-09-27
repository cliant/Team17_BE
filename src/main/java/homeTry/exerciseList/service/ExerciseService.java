package homeTry.exerciseList.service;

import homeTry.exerciseList.model.entity.ExerciseHistory;
import homeTry.exerciseList.repository.ExerciseHistoryRepository;
import homeTry.exerciseList.repository.ExerciseRepository;
import homeTry.exerciseList.model.entity.Exercise;
import homeTry.exerciseList.dto.ExerciseRequest;
import homeTry.exerciseList.repository.ExerciseTimeRepository;
import homeTry.member.dto.MemberDTO;
import homeTry.member.model.entity.Member;
import homeTry.member.model.vo.Email;
import homeTry.member.repository.MemberRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseHistoryRepository exerciseHistoryRepository;
    private final ExerciseTimeRepository exerciseTimeRepository;
    private final MemberRepository memberRepository;

    public ExerciseService(ExerciseRepository exerciseRepository,
        ExerciseHistoryRepository exerciseHistoryRepository,
        ExerciseTimeRepository exerciseTimeRepository, MemberRepository memberRepository) {
        this.exerciseRepository = exerciseRepository;
        this.exerciseHistoryRepository = exerciseHistoryRepository;
        this.exerciseTimeRepository = exerciseTimeRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void createExercise(ExerciseRequest request, MemberDTO memberDTO) {

        Member member = memberRepository.findByEmail(new Email(memberDTO.email()))
            .orElseThrow(() -> new IllegalArgumentException("해당 이메일을 가진 사용자를 찾을 수 없습니다."));

        Exercise exercise = new Exercise(request.exerciseName(), member);
        exerciseRepository.save(exercise);
    }

    @Transactional
    public void deleteExercise(Long exerciseId, MemberDTO memberDTO) {
        Exercise exercise = getExerciseByIdAndMember(exerciseId, memberDTO);
        exercise.markAsDeprecated(); // isDeprecated 값을 true로 설정
        exerciseRepository.save(exercise);
    }

    @Transactional
    public void startExercise(Long exerciseId, MemberDTO memberDTO) {
        Exercise exercise = getExerciseByIdAndMember(exerciseId, memberDTO);
        exercise.startExercise();
        exerciseTimeRepository.save(exercise.getCurrentExerciseTime());
    }

    @Transactional
    public void stopExercise(Long exerciseId, MemberDTO memberDTO) {
        Exercise exercise = getExerciseByIdAndMember(exerciseId, memberDTO);
        exercise.stopExercise();
        exerciseTimeRepository.save(exercise.getCurrentExerciseTime());
    }

    private Exercise getExerciseByIdAndMember(Long exerciseId, MemberDTO memberDTO) {
        Email memberEmail = new Email(memberDTO.email());
        return exerciseRepository.findByIdAndMemberEmail(exerciseId, memberEmail)
            .orElseThrow(() -> new IllegalArgumentException("운동을 찾을 수 없거나 권한이 없습니다."));
    }

    @Transactional(readOnly = true)
    public Duration getWeeklyTotalExercise(String memberEmail) {
        // 이번 주의 시작과 끝 계산 (새벽 3시 기준), 하루 시작: 새벽 3시, 하루 끝: 다음날 새벽 2시 59분 59초
        LocalDate startOfWeek = LocalDate.now()
            .minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDateTime startOfWeekWith3AM = startOfWeek.atTime(3, 0, 0);
        LocalDateTime endOfWeekWith3AM = startOfWeek.plusDays(6).atTime(2, 59, 59);

        List<ExerciseHistory> weeklyExercises = exerciseHistoryRepository.findByExerciseMemberEmailAndCreatedAtBetween(
            new Email(memberEmail), startOfWeekWith3AM, endOfWeekWith3AM);

        return sumExerciseTime(weeklyExercises);
    }

    @Transactional(readOnly = true)
    public Duration getMonthlyTotalExercise(String memberEmail) {
        // 이번 달의 시작과 끝 계산
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDateTime startOfMonthWith3AM = startOfMonth.atTime(3, 0, 0);
        LocalDateTime endOfMonthWith3AM = startOfMonth.plusMonths(1).minusDays(1).atTime(2, 59, 59);

        List<ExerciseHistory> monthlyExercises = exerciseHistoryRepository.findByExerciseMemberEmailAndCreatedAtBetween(
            new Email(memberEmail), startOfMonthWith3AM, endOfMonthWith3AM);

        return sumExerciseTime(monthlyExercises);
    }

    private Duration sumExerciseTime(List<ExerciseHistory> exercises) {
        return exercises.stream()
            .map(ExerciseHistory::getExerciseHistoryTime)
            .reduce(Duration.ZERO, Duration::plus);
    }

}
