package org.xinyo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.xinyo.domain.*;
import org.xinyo.service.AuthorService;
import org.xinyo.service.DailyPoetryService;
import org.xinyo.service.PoetryService;
import org.xinyo.util.WebUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by chengxinyong on 2018/3/27.
 */
@Controller
public class IndexController {

    @Autowired
    private DailyPoetryService dailyPoetryService;

    @Autowired
    private AuthorService authorService;

    @Autowired
    private PoetryService poetryService;

    @Value("${baseweb.description.length}")
    private int descLength;

    @RequestMapping(value = {"/"}, method = RequestMethod.GET)
    public String index(Model model, @CookieValue(name = "language", defaultValue = "1") Integer language) {
        Date day = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dayStr = formatter.format(day);
        DailyPoetry dailyPoetry = dailyPoetryService.selectById(dayStr);

        int poetryId = dailyPoetry == null ? 1391 : dailyPoetry.getPoetryId();

        BaseWeb baseWeb = new BaseWeb();
        Map<String, Object> params = new HashMap<>();
        params.put("id", poetryId);
        params.put("language", language);

        Poetry poetry = poetryService.findByIdAndLanguage(params);

        if (poetry == null) {
            return "404";
        }

        PoetryBean poetryBean = new PoetryBean(poetry);
        if (poetry.getAuthorId() != null) {
            params.put("id", poetry.getAuthorId());
            Author author = authorService.findByIdAndLanguage(params);
            model.addAttribute("author", author);
        }

        List<List<String>> keywords = new ArrayList<>();

        keywords.add(poetryBean.getTags());
        keywords.add(poetryBean.getKeywords());
        baseWeb.setTitle(poetry.getTitle() + " - " + poetry.getAuthor());
        baseWeb.setKeywords(String.join(",", WebUtils.joinList(keywords)));
        baseWeb.setDescription(WebUtils.getDes(poetry.getParagraphs(), descLength));
        baseWeb.setLanguage(language);

        model.addAttribute("poetry", poetryBean);
        model.addAttribute("web", baseWeb);
        return "poetry";
    }


}
