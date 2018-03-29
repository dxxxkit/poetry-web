$(function () {

    var maxId = 332875;
    var randomId = Math.floor(Math.random() * maxId);
    var language = 1; // 默认简体

//写cookies
    function setCookie(name, value) {
        var Days = 30;
        var exp = new Date();
        exp.setTime(exp.getTime() + Days * 24 * 60 * 60 * 1000);
        document.cookie = name + "=" + encodeURI(value) + ";expires=" + exp.toGMTString();
    }

//读取cookies
    function getCookie(name) {
        var arr, reg = new RegExp("(^| )" + name + "=([^;]*)(;|$)");
        if (arr = document.cookie.match(reg))
            return decodeURI(arr[2]);
        else
            return null;
    }

    function buildPoetryPage(poetry) {
        $("meta[name=keywords]").attr("content", poetry.keyWords);
        $("meta[name=description]").attr("content", poetry.description);
        $("title").html(poetry.title);

        var poetryId = "<input id='poetry-id' hidden value='" + poetry.id + "'/>";
        var poetryHeader = "<div id='poetry-header'><div id='poetry-title'><h2>" + poetry.title +
            "</h2></div> <div id='poetry-author'>" + poetry.author + "</div></div>";
        var poetryContent = "<div id='poetry-content'>";
        $.each(poetry.contentList, function (index, value) {
            poetryContent += "<p class='content-p'>" + value + "</p>";
        })
        poetryContent += "</div>";

        $("#poetry").empty();
        $("#poetry").append("<div class='poetry-item poetry-single'>" + poetryId + poetryHeader + poetryContent + "</div>");

        $("#nav-bar").html("<div class='pre-poetry pre-item'>上一篇</div><div class='next-poetry next-item'>下一篇</div><div class='clearfix'></div> ")

        initToolbar();
    }

    function searchPoetryPage(resultMap) {
        let keyword = resultMap.keyword;
        $("meta[name=keywords]").attr("content", keyword);
        $("meta[name=description]").attr("content", keyword + " - 搜索结果");
        $("title").html(keyword + " - 搜索结果");

        $("#poetry").empty();
        $.each(resultMap.poetryBeanList, function (index, poetry) {
            let content = "";
            $.each(poetry.contentList, function (index, value) {
                content += value;
            })
            let re = new RegExp(keyword,"g");
            let item = "<div class='search-item'><span class='search-item-title'>" + poetry.title.replace(re, "<span class='search-keyword'>" + keyword + "</span>") +
                "</span><span class='search-item-author'>[" + poetry.author.replace(re, "<span class='search-keyword'>" + keyword + "</span>") +
                "]</span><div class='search-item-content'>" + content.replace(re, "<span class='search-keyword'>" + keyword + "</span>") +
                "</div></div>";
            $("#poetry").append(item);
        })

        $("#nav-bar").html("<input id='current-page' hidden value='" + resultMap.page + "'>" +
            "<input id='current-keyword' hidden value='" + keyword + "'>" +
            "<div class='pre-page pre-item'>上一页</div><div class='next-page next-item'>下一页</div>" +
            "<div class='clearfix'></div> ")
    }

    function initToolbar() {
        $(".poetry-single").append("<div class='tool-bar'>" +
            "<span class='copy-poetry'><i class='material-icons'>content_copy</i></span></div>")
    }

    function getPoetryById(id, unPushState) {
        $.ajax({
            url: "/api/poetry/" + id,
            method: "GET",
            dataType: "json",
            data: "language=" + language,
            success: function (data) {
                buildPoetryPage(data);
                if (!unPushState) {
                    history.pushState({}, "", "/poetry/" + id);
                }
            }
        })
    }

    function getPoetryByKeyword(keyword, page, unPushState) {
        $.ajax({
            url: "/api/poetry/search",
            method: "GET",
            dataType: "json",
            data: "language=" + language + "&keyword=" + keyword + "&page=" + page,
            success: function (data) {
                searchPoetryPage(data);
                if (!unPushState) {
                    history.pushState({}, "", "/poetry/search?keyword=" + keyword + "&page=" + page);
                }
            }
        })
    }

    function switchLanguage(targetValue) {
        console.log("当前语言模式为：" + (targetValue ? "简体" : "繁体"));
        setCookie("language", targetValue);
        language = targetValue;
        if (targetValue == 1) { // 简
            $("#tr-sp .sp").css("background", "#2bc");
            $("#tr-sp .tr").css("background", "#ccc");
        } else { // 繁
            $("#tr-sp .tr").css("background", "#2bc");
            $("#tr-sp .sp").css("background", "#ccc");
        }
    }

    function loadByUrl() {
        let location = document.location + '';
        let re = /\/poetry\/(\d+)$/i;
        let found = location.match(re);
        if (found){
            getPoetryById(found[1], true);
            return;
        }

        re = /\/poetry\/search\?keyword=([^&]*)&page=(\d*)$/i;
        found = location.match(re);
        if(found){
            getPoetryByKeyword(found[1], found[2], true);
            return;
        }

        getPoetryById(randomId);
    }

// 下一篇
    $(document).on("click", ".next-poetry", function () {
        let currentId = parseInt($("#poetry-id").val());
        if (currentId >= maxId) {
            getPoetryById(maxId);
        } else {
            getPoetryById(currentId + 1);
        }
    })

// 上一篇
    $(document).on("click", ".pre-poetry", function () {
        let currentId = parseInt($("#poetry-id").val());
        if (currentId <= 1) {
            getPoetryById(1);
        } else {
            getPoetryById(currentId - 1);
        }
    })

// 下一页
    $(document).on("click", ".next-page", function () {
        let page = parseInt($("#current-page").val());
        let keyword = $("#current-keyword").val();
        getPoetryByKeyword(keyword, page + 1);
    })


// 上一页
    $(document).on("click", ".pre-page", function () {
        let page = parseInt($("#current-page").val());
        let keyword = $("#current-keyword").val();
        if (page == 1) {
            getPoetryByKeyword(keyword, page);
        } else {
            getPoetryByKeyword(keyword, page - 1);
        }
    })


// 搜索
    $(document).on("click", "#searchsubmit", function () {
        getPoetryByKeyword($("#keyword").val(), 1);
    })
    $(document).on("keyup", "#keyword", function (e) {
        let keycode = 'which' in e ? e.which : e.keyCode;
        if (keycode == "13") { //回车
            getPoetryByKeyword($(this).val(), 1);
        }
    })

// 繁简切换
    $(document).on("click", "#tr-sp", function () {
        switchLanguage(language ^ 1);
    })

// 复制
    $(document).on("click", ".copy-poetry", function () {
        //初始化
        $("textarea").remove("#targetId");

        var poetryHeader = $("#poetry-header");
        var poetryContent = $("#poetry-content");
        var poetryText = poetryHeader[0].outerText + "\n" + poetryContent[0].outerText.replace(/\n\n/g, "\n");

        //添加 <textarea> DOM节点，将获取的代码写入
        var target = document.createElement("textarea");
        target.style.position = "absolute";
        target.style.left = "-9999px";
        target.style.top = "0";
        target.id = "targetId";
        poetryHeader.append(target);
        target.textContent = poetryText;

        //选中textarea内的代码
        target.focus();
        target.setSelectionRange(0, target.value.length);

        // 复制选中的内容
        document.execCommand("copy");

        //删除添加的节点
        $("textarea").remove("#targetId");

        let thisCopied = $(this);
        thisCopied.empty();
        thisCopied.html("<i class='material-icons'>done</i>");
        setTimeout(function () {
            thisCopied.empty();
            thisCopied.html("<i class='material-icons'>content_copy</i>");
        }, 1000)

    })

    window.addEventListener('popstate', function (event) {
        loadByUrl();
    });

    function initAll() {
        // 初始化语言
        let _language = getCookie("language");
        if (_language == null) {
            _language = 1;
        } else {
            _language = parseInt(_language)
        }
        switchLanguage(_language);

        // 初始加载
        loadByUrl();
        console.log(document.location + '')
    }

    initAll();
})