package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.mapper.BookDonationMapper;
import com.example.backend.pojo.BookDonation;
import com.example.backend.service.BookDonationService;
import org.springframework.stereotype.Service;

/**
 * 图书捐赠 Service 实现类
 * 时间: 2026-04-16
 */
@Service
public class BookDonationServiceImpl extends ServiceImpl<BookDonationMapper, BookDonation> implements BookDonationService {
}
