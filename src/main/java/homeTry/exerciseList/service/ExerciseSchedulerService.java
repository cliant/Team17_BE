package homeTry.exerciseList.service;

import homeTry.exerciseList.exception.badRequestException.DailyExerciseTimeLimitExceededException;
import homeTry.exerciseList.exception.badRequestException.ExerciseTimeLimitExceededException;
import homeTry.exerciseList.model.entity.Exercise;
import homeTry.exerciseList.model.entity.ExerciseTime;
import homeTry.member.dto.MemberDTO;
import homeTry.member.service.MemberService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExerciseSchedulerService {

    private final ExerciseService exerciseService;
    private final ExerciseTimeService exerciseTimeService;
    private final ExerciseHistoryService exerciseHistoryService;
    private final ExerciseTimeHelper exerciseTimeHelper;
    private final MemberService memberService;

    public ExerciseSchedulerService(ExerciseService exerciseService,
        ExerciseTimeService exerciseTimeService,
        ExerciseHistoryService exerciseHistoryService,
        ExerciseTimeHelper exerciseTimeHelper,
        MemberService memberService) {
        this.exerciseService = exerciseService;
        this.exerciseTimeService = exerciseTimeService;
        this.exerciseHistoryService = exerciseHistoryService;
        this.exerciseTimeHelper = exerciseTimeHelper;
        this.memberService = memberService;
    }

    // 매일 새벽 3시에 실행
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void saveAllExerciseHistoryAt3AM() {
        List<Exercise> allExercises = exerciseService.findAllNonDeprecatedExercises();
        Set<Long> membersWithExerciseTime = new HashSet<>();

        // 모든 운동 기록을 히스토리에 저장하고 운동 시간을 초기화
        allExercises.forEach(exercise -> {
            ExerciseTime exerciseTime = exerciseTimeService.getExerciseTime(
                exercise.getExerciseId());

            // ExerciseTime 이 없는 경우 넘어감
            if (exerciseTime == null) {
                return;
            }

            // 3시에도 운동이 실행 중이면 강제로 멈추고 exerciseTime 저장
            if (exerciseTime.isActive()) {
                try {
                    exerciseService.stopExercise(exercise.getExerciseId(),
                        MemberDTO.from(exercise.getMember()));
                } catch (ExerciseTimeLimitExceededException |
                         DailyExerciseTimeLimitExceededException e) {
                    // 예외가 발생해도 스케줄러는 중단되지 않고 다음 운동을 처리
                    System.out.println(
                        "운동 시간이 초과되어 강제 종료되었습니다. Exercise ID: " + exercise.getExerciseId());
                }
                exerciseTimeHelper.saveExerciseTime(exerciseTime);
            }

            // 운동 시간이 있는 멤버 ID 저장
            if (!exerciseTime.getExerciseTime().isZero()) {
                membersWithExerciseTime.add(exercise.getMember().getId());
            }

            // exerciseTime -> exerciseHistory 이동
            exerciseHistoryService.saveExerciseHistory(exerciseTime.getExercise(), exerciseTime);

            exerciseTimeService.resetDailyExercise(exerciseTime);
        });

        // 운동 시간이 있는 멤버들의 출석일 증가
        membersWithExerciseTime.forEach(memberService::incrementAttendanceDate);
    }

}
