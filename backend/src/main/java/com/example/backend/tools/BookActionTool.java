package com.example.backend.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.controller.ActionController;
import com.example.backend.pojo.Book;
import com.example.backend.pojo.Result;
import com.example.backend.service.BookService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图书借阅和预约工具
 * 时间: 2026-04-15
 */
@Component
public class BookActionTool {

    @Autowired
    private BookService bookService;

    @Autowired
    private ActionController actionController;

    /**
     * 借阅图书工具
     * 时间: 2026-04-15
     *
     * @param title 书名
     * @return 借阅结果或提示信息
     */
    @Tool("如果用户需要借阅图书且提供了具体的《书名》，调用此工具帮助用户借阅该书。如果该书库存为0，此工具会返回库存不足的提示，此时你需要向用户提问是否要预约。")
    public String borrowBook(String title) {
        try {
            // 根据书名精确查询图书
            QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("title", title);
            List<Book> books = bookService.list(queryWrapper);

            if (books == null || books.isEmpty()) {
                return "借阅失败：未找到名为《" + title + "》的图书。";
            }
            if (books.size() > 1) {
                return "找到多本名为《" + title + "》的图书，请提供更具体的信息（如作者等）以便借阅。";
            }

            Book book = books.get(0);
            
            if (book.getStock() == null || book.getStock() <= 0) {
                return "该书库存为0,是否需要预约";
            }
            
            // 复用 ActionController 的借书接口
            Result<String> result = actionController.borrowBook(book.getId());
            
            if (result.getCode() == 200) {
                return "成功借阅《" + title + "》。";
            } else {
                return "借阅《" + title + "》失败：" + result.getMessage();
            }
        } catch (Exception e) {
            return "借阅过程中发生错误：" + e.getMessage();
        }
    }

    /**
     * 预约图书工具
     * 时间: 2026-04-15
     *
     * @param title 书名
     * @return 预约结果或提示信息
     */
    @Tool("如果用户在借书库存不足时，表示想要预约该书，或者直接要求预约某本书且提供了具体的《书名》，调用此工具帮助用户预约该书。")
    public String reserveBook(String title) {
        try {
            // 根据书名精确查询图书
            QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("title", title);
            List<Book> books = bookService.list(queryWrapper);

            if (books == null || books.isEmpty()) {
                return "预约失败：未找到名为《" + title + "》的图书。";
            }
            if (books.size() > 1) {
                return "找到多本名为《" + title + "》的图书，请提供更具体的信息以便预约。";
            }

            Book book = books.get(0);
            
            // 复用 ActionController 的预约接口
            Result<String> result = actionController.reserveBook(book.getId());
            
            if (result.getCode() == 200) {
                return "成功预约《" + title + "》。";
            } else {
                return "预约《" + title + "》失败：" + result.getMessage();
            }
        } catch (Exception e) {
            return "预约过程中发生错误：" + e.getMessage();
        }
    }
}
