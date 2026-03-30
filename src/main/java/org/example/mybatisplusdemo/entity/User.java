package org.example.mybatisplusdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer age;
    private String email;

    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)  // 插入时填充
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)  // 插入和更新时填充
    private Date updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)  // 插入时填充默认值
    private Integer deleted;

    @Version
    @TableField(fill = FieldFill.INSERT)  // 插入时填充默认值
    private Integer version;

    @TableField(exist = false)
    private List<Order> orders;
}