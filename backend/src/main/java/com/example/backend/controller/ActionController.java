package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.pojo.Book;
import com.example.backend.pojo.BorrowRecord;
import com.example.backend.pojo.ReserveRecord;
import com.example.backend.pojo.Result;
import com.example.backend.service.BookService;
import com.example.backend.service.BorrowRecordService;
import com.example.backend.service.ReserveRecordService;
import com.example.backend.service.UserService;
import com.example.backend.pojo.User;
import com.example.backend.tools.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 借阅/预约控制器
 * 时间: 2026-03-28
 */
@RestController
@RequestMapping("/action")
public class ActionController {

    @Autowired
    private BookService bookService;

    @Autowired
    private BorrowRecordService borrowRecordService;

    @Autowired
    private ReserveRecordService reserveRecordService;

    @Autowired
    private UserService userService;

    /**
     * 借书
     * 时间: 2026-03-30
     *
     * @param bookId 图书ID
     * @return 借阅结果
     */
    @PostMapping("/borrow/{bookId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> borrowBook(@PathVariable Long bookId) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        // 检查是否已借阅且未归还
        // 时间: 2026-03-30
        QueryWrapper<BorrowRecord> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("user_id", info.getUserId())
                    .eq("book_id", bookId)
                    .eq("status", "BORROWED");
        if (borrowRecordService.count(checkWrapper) > 0) {
            return Result.error("该书已借阅且未归还，不可重复借阅");
        }

        Book book = bookService.getById(bookId);
        if (book == null) return Result.error("图书不存在");
        if (book.getStock() <= 0) return Result.error("库存不足，请预约");

        // 扣减库存，增加借出数量
        book.setStock(book.getStock() - 1);
        book.setBorrowedCount(book.getBorrowedCount() + 1);
        bookService.updateById(book);

        // 生成借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setUserId(info.getUserId());
        record.setBookId(bookId);
        record.setBorrowDate(new Date());
        record.setStatus("BORROWED");
        borrowRecordService.save(record);

        return Result.success("借阅成功");
    }

    /**
     * 还书
     *
     * @param recordId 借阅记录ID
     * @return 还书结果
     */
    @PostMapping("/return/{recordId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> returnBook(@PathVariable Long recordId) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        BorrowRecord record = borrowRecordService.getById(recordId);
        if (record == null) return Result.error("记录不存在");
        if (!"BORROWED".equals(record.getStatus())) return Result.error("该书已归还");
        
        // 如果不是管理员，且不是自己的记录，拒绝
        if (!"admin".equals(info.getRole()) && !record.getUserId().equals(info.getUserId())) {
            return Result.error(403, "无权限");
        }

        record.setStatus("RETURNED");
        record.setReturnDate(new Date());
        borrowRecordService.updateById(record);

        Book book = bookService.getById(record.getBookId());
        if (book != null) {
            book.setStock(book.getStock() + 1);
            bookService.updateById(book);
        }

        return Result.success("归还成功");
    }

    /**
     * 预约
     * 时间: 2026-03-30
     *
     * @param bookId 图书ID
     * @return 预约结果
     */
    @PostMapping("/reserve/{bookId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> reserveBook(@PathVariable Long bookId) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        Book book = bookService.getById(bookId);
        if (book == null) return Result.error("图书不存在");
        if (book.getStock() > 0) return Result.error("该书有库存，可直接借阅");

        // 检查是否已预约
        // 时间: 2026-03-30
        QueryWrapper<ReserveRecord> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("user_id", info.getUserId())
                    .eq("book_id", bookId)
                    .eq("status", "RESERVED");
        if (reserveRecordService.count(checkWrapper) > 0) {
            return Result.error("《" + book.getTitle() + "》已经预约过了，不能重复预约");
        }

        // 增加预约数量
        book.setReservedCount(book.getReservedCount() + 1);
        bookService.updateById(book);

        ReserveRecord record = new ReserveRecord();
        record.setUserId(info.getUserId());
        record.setBookId(bookId);
        record.setReserveDate(new Date());
        record.setStatus("RESERVED");
        reserveRecordService.save(record);

        return Result.success("预约成功");
    }

    /**
     * 获取个人借阅记录 (仅返回已归还的记录，并包含图书详细信息)
     *
     * @return 借阅记录列表
     */
    @GetMapping("/borrow/list")
    public Result<List<BorrowRecord>> myBorrowList() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        QueryWrapper<BorrowRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", info.getUserId());
        queryWrapper.eq("status", "RETURNED");
        List<BorrowRecord> records = borrowRecordService.list(queryWrapper);

        for (BorrowRecord record : records) {
            Book book = bookService.getById(record.getBookId());
            if (book != null) {
                record.setTitle(book.getTitle());
                record.setAuthor(book.getAuthor());
                record.setPublisher(book.getPublisher());
            }
        }

        return Result.success(records);
    }

    /**
     * 获取个人未归还借阅记录 (状态为 BORROWED，并包含图书详细信息)
     *
     * @return 未归还借阅记录列表
     */
    @GetMapping("/borrow/unreturned/list")
    public Result<List<BorrowRecord>> myUnreturnedBorrowList() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        QueryWrapper<BorrowRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", info.getUserId());
        queryWrapper.eq("status", "BORROWED");
        List<BorrowRecord> records = borrowRecordService.list(queryWrapper);

        for (BorrowRecord record : records) {
            Book book = bookService.getById(record.getBookId());
            if (book != null) {
                record.setTitle(book.getTitle());
                record.setAuthor(book.getAuthor());
                record.setPublisher(book.getPublisher());
            }
        }

        return Result.success(records);
    }

    /**
     * 获取所有借阅记录 (管理员)
     * 时间: 2026-03-30
     *
     * @return 借阅记录列表
     */
    @GetMapping("/borrow/all")
    public Result<List<BorrowRecord>> allBorrowList() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");
        if (!"admin".equals(info.getRole())) return Result.error(403, "无权限");

        List<BorrowRecord> records = borrowRecordService.list();
        for (BorrowRecord record : records) {
            Book book = bookService.getById(record.getBookId());
            if (book != null) {
                record.setTitle(book.getTitle());
                record.setAuthor(book.getAuthor());
                record.setPublisher(book.getPublisher());
            }
            User user = userService.getById(record.getUserId());
            if (user != null) {
                record.setUsername(user.getUsername());
            }
        }
        return Result.success(records);
    }

    /**
     * 新建一条借阅记录 (管理员)
     * 时间: 2026-03-30
     *
     * @param record 借阅记录信息
     * @return 操作结果
     */
    @PostMapping("/borrow/create")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> createBorrowRecord(@RequestBody BorrowRecord record) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");
        if (!"admin".equals(info.getRole())) return Result.error(403, "无权限");

        if (record.getUsername() == null || record.getTitle() == null) {
            return Result.error("用户名和书名不能为空");
        }

        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.eq("username", record.getUsername());
        User user = userService.getOne(userQuery);
        if (user == null) return Result.error("用户不存在");

        QueryWrapper<Book> bookQuery = new QueryWrapper<>();
        bookQuery.eq("title", record.getTitle());
        Book book = bookService.getOne(bookQuery);
        if (book == null) return Result.error("图书不存在");
        
        // 检查是否已借阅且未归还
        // 时间: 2026-03-30
        QueryWrapper<BorrowRecord> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("user_id", user.getId())
                    .eq("book_id", book.getId())
                    .eq("status", "BORROWED");
        if (borrowRecordService.count(checkWrapper) > 0) {
            // 返回具体书名的时间: 2026-03-30
            return Result.error("《" + book.getTitle() + "》已借阅且未归还，不可重复借阅");
        }

        if (book.getStock() <= 0) {
            // 返回具体书名的时间: 2026-03-30
            return Result.error("《" + book.getTitle() + "》库存不足，请预约");
        }

        // 扣减库存，增加借出数量
        book.setStock(book.getStock() - 1);
        book.setBorrowedCount(book.getBorrowedCount() + 1);
        bookService.updateById(book);

        record.setUserId(user.getId());
        record.setBookId(book.getId());

        if (record.getBorrowDate() == null) {
            record.setBorrowDate(new Date());
        }
        if (record.getStatus() == null) {
            record.setStatus("BORROWED");
        }

        borrowRecordService.save(record);
        return Result.success("创建借阅记录成功");
    }

    /**
     * 新建一条预约记录 (管理员)
     * 时间: 2026-03-30
     *
     * @param record 预约记录信息
     * @return 操作结果
     */
    @PostMapping("/reserve/create")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> createReserveRecord(@RequestBody ReserveRecord record) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");
        if (!"admin".equals(info.getRole())) return Result.error(403, "无权限");

        if (record.getUsername() == null || record.getTitle() == null) {
            return Result.error("用户名和书名不能为空");
        }

        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.eq("username", record.getUsername());
        User user = userService.getOne(userQuery);
        if (user == null) return Result.error("用户不存在");

        QueryWrapper<Book> bookQuery = new QueryWrapper<>();
        bookQuery.eq("title", record.getTitle());
        Book book = bookService.getOne(bookQuery);
        if (book == null) return Result.error("图书不存在");

        // 检查是否已预约
        // 时间: 2026-03-30
        QueryWrapper<ReserveRecord> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("user_id", user.getId())
                    .eq("book_id", book.getId())
                    .eq("status", "RESERVED");
        if (reserveRecordService.count(checkWrapper) > 0) {
            return Result.error("《" + book.getTitle() + "》已经预约过了，不能重复预约");
        }

        // 增加预约数量
        book.setReservedCount(book.getReservedCount() + 1);
        bookService.updateById(book);

        record.setUserId(user.getId());
        record.setBookId(book.getId());

        if (record.getReserveDate() == null) {
            record.setReserveDate(new Date());
        }
        if (record.getStatus() == null) {
            record.setStatus("RESERVED");
        }

        reserveRecordService.save(record);
        return Result.success("创建预约记录成功");
    }
}
