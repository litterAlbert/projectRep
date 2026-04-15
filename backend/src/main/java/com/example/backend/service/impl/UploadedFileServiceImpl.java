package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.mapper.UploadedFileMapper;
import com.example.backend.pojo.UploadedFile;
import com.example.backend.service.UploadedFileService;
import org.springframework.stereotype.Service;

/**
 * 上传文件记录ServiceImpl类
 * 时间: 2026-04-15
 */
@Service
public class UploadedFileServiceImpl extends ServiceImpl<UploadedFileMapper, UploadedFile> implements UploadedFileService {
}