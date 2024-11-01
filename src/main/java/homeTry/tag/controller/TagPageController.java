package homeTry.tag.controller;

import homeTry.tag.productTag.dto.ProductTagDto;
import homeTry.tag.productTag.dto.response.ProductTagResponse;
import homeTry.tag.productTag.service.ProductTagService;
import homeTry.tag.teamTag.dto.TeamTagDTO;
import homeTry.tag.teamTag.service.TeamTagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/tag")
public class TagPageController {

    private final TeamTagService teamTagService;
    private final ProductTagService productTagService;

    public TagPageController(TeamTagService teamTagService, ProductTagService productTagService) {
        this.teamTagService = teamTagService;
        this.productTagService = productTagService;
    }

    @GetMapping("/team")
    public String showTeamTagList(Model model) {
        List<ProductTagDto> productTags = productTagService.getProductTagList().productTags();
        model.addAttribute("productTags", productTags);
        return "ProductTags";
    }

    @GetMapping("/product")
    public String showProductTagList(Model model) {
        List<TeamTagDTO> teamTags = teamTagService.getAllTeamTagList();
        model.addAttribute("teamtags", teamTags);
        return "TeamTags";
    }
}
