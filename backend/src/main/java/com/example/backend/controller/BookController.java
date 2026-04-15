package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.pojo.Book;
import com.example.backend.pojo.Result;
import com.example.backend.service.BookService;
import com.example.backend.tools.BookRecommendationTool;
import com.example.backend.tools.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书控制器
 * 时间: 2026-03-28
 */
@RestController
@RequestMapping("/book")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRecommendationTool bookRecommendationTool;

    /**
     * 添加图书 (管理员)
     *
     * @param book 图书信息
     * @return 添加结果
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Book book) {
        checkAdmin();
        bookService.save(book);
        // 新增图书后，同步到推荐系统的向量数据库
        bookRecommendationTool.addBookToVectorDb(book);
        return Result.success("添加成功");
    }

    /**
     * 删除图书 (管理员)
     *
     * @param id 图书ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        checkAdmin();
        System.out.println(id);
        bookService.removeById(id);
        // 同步从推荐系统的向量数据库中删除该图书
        bookRecommendationTool.removeBookFromVectorDb(id);
        return Result.success("删除成功");
    }

    /**
     * 更新图书信息 (管理员)
     *
     * @param book 图书信息
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody Book book) {
        checkAdmin();
        bookService.updateById(book);
        // 同步更新推荐系统的向量数据库
        bookRecommendationTool.updateBookInVectorDb(book);
        return Result.success("更新成功");
    }

    /**
     * 检索图书
     * 支持书名、作者、ISBN模糊匹配
     *
     * @param keyword 关键词
     * @return 图书列表
     */
    @GetMapping("/search")
    public Result<List<Book>> search(@RequestParam(required = false) String keyword) {
        QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("title", keyword)
                    .or().like("author", keyword)
                    .or().like("isbn", keyword);
        }
        List<Book> list = bookService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 获取图书详情
     *
     * @param id 图书ID
     * @return 图书详情
     */
    @GetMapping("/{id}")
    public Result<Book> getById(@PathVariable Long id) {
        return Result.success(bookService.getById(id));
    }

    private void checkAdmin() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null || !"admin".equals(info.getRole())) {
            throw new RuntimeException("无权限操作");
        }
    }
}
