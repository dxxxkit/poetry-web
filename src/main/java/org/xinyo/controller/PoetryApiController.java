package org.xinyo.controller;

import com.google.gson.reflect.TypeToken;
import com.hankcs.hanlp.HanLP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.xinyo.domain.Author;
import org.xinyo.domain.Poetry;
import org.xinyo.domain.PoetryBean;
import org.xinyo.domain.SearchResult;
import org.xinyo.service.AuthorService;
import org.xinyo.service.PoetryService;
import org.xinyo.service.SearchResultService;
import org.xinyo.util.JsonUtils;
import org.xinyo.util.UnicodeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.xinyo.util.PoetryUtils.poetry2PoetryBean;

/**
 * Created by chengxinyong on 2018/3/27.
 */
@RestController
public class PoetryApiController {

    @Autowired
    private PoetryService poetryService;

    @Autowired
    private AuthorService authorService;

    @Autowired
    private SearchResultService searchResultService;

    @RequestMapping(value = "/api/poetry/{id}", method = RequestMethod.GET)
    public Map<String, Object> getPoetryById(@PathVariable Integer id, @RequestParam Integer language) {
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("language", language);

        Poetry poetry = poetryService.findByIdAndLanguage(params);

        if (poetry == null) {
            poetry = new Poetry(id);
        }

        PoetryBean poetryBean = poetry2PoetryBean(poetry);
        if (poetry.getAuthorId() != null) {
            params.put("id", poetry.getAuthorId());
            Author author = authorService.findByIdAndLanguage(params);
            resultMap.put("author", author);
        }

//        createKeywords(id);
//        new Thread(() -> replaceWenHao()).start();
//        new Thread(() -> createTags(id)).start();

        resultMap.put("poetry", poetryBean);
        return resultMap;
    }

    @RequestMapping(value = "/api/poetry/search", method = RequestMethod.GET)
    public Map getPoetryByKeyword(@RequestParam String keyword, @RequestParam Integer page, @RequestParam Integer language) {
        Map<String, Object> resultMap = new HashMap<>();

        if (page == null) {
            page = 1;
        }

        keyword = UnicodeUtils.transBaseUnicode(keyword);
        List<Poetry> poetryList;
        int total = 0;
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("page", (page - 1) * 10);
        params.put("language", language);

        // 1.读结果表
        SearchResult searchResult = searchResultService.findByKeyword(keyword);

        // 2.根据结果查询
        if (searchResult == null) {
            // 2.1 重新查询poetry表
            total = poetryService.countTotalPoetryByKeyword(params);

            if (total > 0L) {
                poetryList = poetryService.findByKeywordAndLanguage(params);
            } else {
                return null;
            }

            long finalTotal = total;
            String finalKeyword = keyword;
            new Thread(() -> addNewSearchResult(finalKeyword, (int) finalTotal)).start();

        } else {
            // 2.2 根据searchResult表进行查询
            total = searchResult.getTotal();
            if (page > (int) Math.ceil(total / 10d)) return null;

            if (page <= 10) {
                // 只需取索引查询
                String top100Id = searchResult.getTop100Id();
                List<String> list = JsonUtils.jsonToList(top100Id, new TypeToken<ArrayList<String>>() {
                }.getType());
                List<String> idList = list.subList((page - 1) * 10, Math.min(page * 10, total));

                if (language == 0) { // 繁体
                    poetryList = poetryService.findTrByIds(idList);
                } else { // 简体
                    poetryList = poetryService.findSpByIds(idList);
                }
            } else {
                // 重新查询
                poetryList = poetryService.findByKeywordAndLanguage(params);
            }
        }

        List<PoetryBean> poetryBeanList = poetry2PoetryBean(poetryList);

        resultMap.put("poetryBeanList", poetryBeanList);
        resultMap.put("keyword", language == 0 ? HanLP.convertToTraditionalChinese(keyword) : keyword);
        resultMap.put("page", page);
        resultMap.put("total", total);

        return resultMap;
    }

    /**
     * 缓存搜索结果
     *
     * @param keyword
     * @param total
     */
    private void addNewSearchResult(String keyword, int total) {
        SearchResult newResult = new SearchResult();
        newResult.setKeyword(keyword);
        newResult.setTotal(total);
        newResult.setTop100Id(JsonUtils.toJson(poetryService.findTop100IdByKeyword(keyword)));
        searchResultService.add(newResult);
    }

    /**
     * 自动生成标签
     */
    private void createTags(int id) {
        Map<String, Object> params = new HashMap<>();
        String yuefuName = "(郊庙歌辞|燕射歌辞|鼓吹曲辞|横吹曲辞|相和歌辞|清商曲辞|舞曲歌辞|琴曲歌辞|杂曲歌辞|近代曲辞|杂歌谣辞|新乐府辞)";
        params.put("language", 1);
        for (int i = 1; i <= 332875; i++) {
            params.put("id", i);
            Poetry poetry = poetryService.findByIdAndLanguage(params);
            if(poetry == null) continue;
            String title = poetry.getTitle();
            String tags = poetry.getTags();
            List tagList;
            if(tags == null || tags.equals("")) {
                tagList = new ArrayList();
            } else {
                tagList = JsonUtils.jsonToList(tags);
            }

            if (title.matches(yuefuName + ".*")) {
                // 匹配到乐府标题
                String tagName = title.replaceAll(yuefuName + ".*", "$1");

                boolean flag = false;
                if (!tagList.contains(tagName)) {
                    tagList.add(0, tagName);
                    flag = true;
                }
                if (!tagList.contains("乐府")) {
                    tagList.add(0, "乐府");
                    flag = true;
                }

                if (flag) {
                    params.put("tags", JsonUtils.toJson(tagList));
                    int result = poetryService.updateTagsById(params);
                    if (result > 0) {
                        System.err.println(params);
                    }
                }
            }else {
                System.out.println("...");
            }
        }
    }

    /**
     * 自动生成关键词
     *
     * @param id
     */
    private void createKeywords(int id) {
        Map<String, Object> params = new HashMap<>();
        params.put("language", 1);
        for (int i = id; i <= id; i++) {
            params.put("id", i);
            Poetry poetry = poetryService.findByIdAndLanguage(params);
            String s = poetry.getTitle() + ", " + poetry.getParagraphs();
            List<String> strings = HanLP.extractKeyword(s, 32);
            List<String> list = new ArrayList<>();
            for (String string : strings) {
                if (string.length() == 2) {
                    list.add(string);
                }
            }

            String json = JsonUtils.toJson(list);

            params.put("keywords", json);
            poetryService.updateKeywordsById(params);
            System.err.println(i);
        }

    }

    /**
     * 字符集不支持，乱码4个问好替换
     */
    private void replaceWenHao() {

        // 1. 查询简体结果
        for (int i : wenhaoId) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", i);
            params.put("language", 1);

//            Poetry sp = poetryService.findByIdAndLanguage(params);
            Author sp = authorService.findByIdAndLanguage(params);

            params.put("language", 0);
            Author tr = authorService.findByIdAndLanguage(params);

            String spStr = sp.getDesc();
            String trStr = tr.getDesc();

            String spStrResult = getReplaced(spStr, trStr);


            params.put("title", spStrResult);
            System.out.println(i + ": " + spStrResult);
            authorService.updateDescSpById(params);
        }

    }

    private String getReplaced(String source, String base) {
        int wenhaoIndex = source.indexOf("????", 0);
        if (wenhaoIndex != -1) {
            String targetStr = base.substring(wenhaoIndex, wenhaoIndex + 1);
            String spStrResult = source.replaceFirst("[?]{4}", targetStr);
            return getReplaced(spStrResult, base);
        } else {
            return source;
        }
    }


    private static int[] wenhaoId = new int[]{};


}
