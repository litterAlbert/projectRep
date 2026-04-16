package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.pojo.Book;
import com.example.backend.pojo.BookDonation;
import com.example.backend.pojo.Result;
import com.example.backend.pojo.User;
import com.example.backend.service.BookDonationService;
import com.example.backend.service.BookService;
import com.example.backend.service.UserService;
import com.example.backend.tools.BookRecommendationTool;
import com.example.backend.tools.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 图书捐赠控制器
 * 时间: 2026-04-16
 */
@RestController
@RequestMapping("/donation")
public class BookDonationController {

    @Autowired
    private BookDonationService bookDonationService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookRecommendationTool bookRecommendationTool;

    /**
     * 提交图书捐赠 (用户)
     * 时间: 2026-04-16
     *
     * @param donation 捐赠信息
     * @return 提交结果
     */
    @PostMapping("/submit")
    public Result<String> submitDonation(@RequestBody BookDonation donation) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) {
            return Result.error(401, "未登录");
        }

        if (donation.getIsbn() == null || donation.getTitle() == null || donation.getAuthor() == null) {
            return Result.error("ISBN、书名和作者不能为空");
        }

        donation.setUserId(info.getUserId());
        donation.setStatus("PENDING");
        donation.setCreatedAt(new Date());
        
        if (donation.getCount() == null || donation.getCount() <= 0) {
            donation.setCount(1);
        }

        bookDonationService.save(donation);
        return Result.success("捐赠申请提交成功，等待管理员处理");
    }

    /**
     * 获取个人的捐赠记录
     * 时间: 2026-04-16
     *
     * @return 个人捐赠记录列表
     */
    @GetMapping("/my")
    public Result<List<BookDonation>> myDonations() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) {
            return Result.error(401, "未登录");
        }

        QueryWrapper<BookDonation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", info.getUserId()).orderByDesc("created_at");
        List<BookDonation> list = bookDonationService.list(queryWrapper);

        return Result.success(list);
    }

    /**
     * 获取所有用户的捐赠记录 (管理员)
     * 时间: 2026-04-16
     *
     * @return 所有捐赠记录列表
     */
    @GetMapping("/list")
    public Result<List<BookDonation>> listAll() {
        checkAdmin();
        QueryWrapper<BookDonation> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_at");
        List<BookDonation> list = bookDonationService.list(queryWrapper);
        
        for (BookDonation donation : list) {
            if (donation.getUserId() != null) {
                User user = userService.getById(donation.getUserId());
                if (user != null) {
                    donation.setUsername(user.getUsername());
                }
            }
        }
        
        return Result.success(list);
    }

    /**
     * 管理员处理旧书入库操作
     * 时间: 2026-04-16
     *
     * @param id 捐赠记录ID
     * @param location 馆藏位置
     * @return 处理结果
     */
    @PostMapping("/process/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> processDonation(@PathVariable Long id, @RequestParam String location) {
        checkAdmin();

        BookDonation donation = bookDonationService.getById(id);
        if (donation == null) {
            return Result.error("捐赠记录不存在");
        }

        if (!"PENDING".equals(donation.getStatus())) {
            return Result.error("该记录已处理");
        }

        if (location == null || location.trim().isEmpty()) {
            return Result.error("入库时馆藏位置不能为空");
        }

        // 检查库中是否已有该ISBN的图书
        QueryWrapper<Book> bookQueryWrapper = new QueryWrapper<>();
        bookQueryWrapper.eq("isbn", donation.getIsbn());
        Book existingBook = bookService.getOne(bookQueryWrapper);

        if (existingBook != null) {
            // 增加库存
            existingBook.setStock(existingBook.getStock() + donation.getCount());
            // 如果位置变化可以更新，这里保留原位置或追加
            bookService.updateById(existingBook);
            bookRecommendationTool.updateBookInVectorDb(existingBook);
        } else {
            // 新增图书
            Book newBook = new Book();
            newBook.setIsbn(donation.getIsbn());
            newBook.setTitle(donation.getTitle());
            newBook.setAuthor(donation.getAuthor());
            newBook.setPublisher(donation.getPublisher());
            newBook.setPublishDate(donation.getPublishDate());
            newBook.setStock(donation.getCount());
            newBook.setBorrowedCount(0);
            newBook.setReservedCount(0);
            newBook.setLocation(location);
            newBook.setCreatedAt(new Date());

            bookService.save(newBook);
            bookRecommendationTool.addBookToVectorDb(newBook);
        }

        // 更新捐赠记录状态
        donation.setStatus("PROCESSED");
        donation.setUpdatedAt(new Date());
        bookDonationService.updateById(donation);

        return Result.success("入库成功");
    }

    /**
     * 管理员拒绝捐赠申请
     * 时间: 2026-04-16
     *
     * @param id 捐赠记录ID
     * @return 处理结果
     */
    @PostMapping("/reject/{id}")
    public Result<String> rejectDonation(@PathVariable Long id) {
        checkAdmin();

        BookDonation donation = bookDonationService.getById(id);
        if (donation == null) {
            return Result.error("捐赠记录不存在");
        }

        if (!"PENDING".equals(donation.getStatus())) {
            return Result.error("该记录已处理");
        }

        donation.setStatus("REJECTED");
        donation.setUpdatedAt(new Date());
        bookDonationService.updateById(donation);

        return Result.success("已拒绝该捐赠");
    }

    private void checkAdmin() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null || !"admin".equals(info.getRole())) {
            throw new RuntimeException("无权限操作");
        }
    }
}
