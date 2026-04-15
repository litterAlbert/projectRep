package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.pojo.UploadedFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 上传文件记录Mapper接口
 * 时间: 2026-04-15
 */
@Mapper
public interface UploadedFileMapper extends BaseMapper<UploadedFile> {
}