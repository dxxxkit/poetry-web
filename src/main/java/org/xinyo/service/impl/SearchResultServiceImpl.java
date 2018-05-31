package org.xinyo.service.impl;

import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xinyo.dao.SearchResultDao;
import org.xinyo.domain.Poetry;
import org.xinyo.domain.SearchResult;
import org.xinyo.domain.TagRelation;
import org.xinyo.service.PoetryService;
import org.xinyo.service.SearchResultService;
import org.xinyo.service.TagRelationService;
import org.xinyo.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by chengxinyong on 2018/3/30.
 */
@Service
public class SearchResultServiceImpl implements SearchResultService {
    @Autowired
    private SearchResultDao searchResultDao;

    @Autowired
    private PoetryService poetryService;

    @Autowired
    private TagRelationService tagRelationService;

    @Override
    public SearchResult findByKeyword(String keyword) {
        return searchResultDao.findByKeyword(keyword);
    }

    @Override
    public int add(SearchResult newResult) {
        return searchResultDao.insert(newResult);
    }

    @Override
    public void addNewSearchResult(String keyword, int total) {
        SearchResult newResult = new SearchResult();
        newResult.setKeyword(keyword);
        newResult.setTotal(total);
        newResult.setTop100Id(JsonUtils.toJson(poetryService.findTop100IdByKeyword(keyword)));
        add(newResult);
    }

    @Override
    public Map<String, Object> searchByKeyword(Map<String, Object> params) {
        Map<String, Object> resultMap = new HashMap<>();

        List<Poetry> poetryList = new ArrayList<>();
        int total = 0;
        String keyword = (String) params.get("keyword");
        int page = (int) params.get("page");
        int language = (int) params.get("language");

        params.put("page", (page - 1) * 10);

        // 1.读结果表
        SearchResult searchResult = findByKeyword(keyword);

        // 2.根据结果查询
        if (searchResult == null) {
            // 2.1 重新查询poetry表
            total = poetryService.countTotalPoetryByKeyword(params);

            if (total > 0) {
                poetryList = poetryService.findByKeywordAndLanguage(params);
                int finalTotal = total;
                new Thread(() -> addNewSearchResult(keyword, finalTotal)).start();
            }

        } else {
            // 2.2 根据searchResult表进行查询
            total = searchResult.getTotal();
            if (page <= (int) Math.ceil(total / 10d)) {
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

        }

        long t1 = System.currentTimeMillis();

        // 3. 查询关联标签
        if (keyword != null) {
            if (!(keyword.startsWith("author:") || keyword.startsWith("tag:"))) {
                // 非作者或者人工标注的标签，才进行查询
                Map<String, Integer> relationMap = tagRelationService.selectByKeyword(keyword);
                resultMap.put("relationTag", relationMap);
            }
        }
        long t2 = System.currentTimeMillis();
        System.err.println("time cost: " + (t2 - t1));

        resultMap.put("data", poetryList);
        resultMap.put("total", total);

        return resultMap;
    }
}
