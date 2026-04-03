package com.nowcoder.community.controller;

import com.nowcoder.community.service.DataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

/**
 * @Author: 19599
 * @Date: 2025/3/2 21:41
 * @Description:
 */
@Controller
public class DataController {

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * 统计页面
     *
     * @return
     */
    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "site/admin/data";
    }

    /**
     * 统计网站UV
     *
     * @param start 开始日期
     * @param end   结束日期
     * @param model
     * @return
     */
    @PostMapping(path = "/data/uv")
    public String getDataUvPage(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                                @DateTimeFormat(pattern = "yyyy-MM-dd") Date end,
                                Model model) {
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);
        return "forward:/data";
    }


    /**
     * 统计活跃用户
     *
     * @param start 开始日期
     * @param end   结束日期
     * @param model
     * @return
     */
    @PostMapping(path = "/data/dau")
    public String getDataDauPage(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                                 @DateTimeFormat(pattern = "yyyy-MM-dd") Date end,
                                 Model model) {
        long dau = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult", dau);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        return "forward:/data";
    }
}
