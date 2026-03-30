package org.example.mybatisplusdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;  // 添加这个导入
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("orders")
@JsonIgnoreProperties(ignoreUnknown = true)  // 添加这行，忽略未知字段
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;  // 关联用户ID

    private BigDecimal totalAmount;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;

    // 一对一关联：订单所属的用户
    @TableField(exist = false)  // 数据库不存在这个字段
    private User user;

    // 状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_SHIPPED = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_CANCELLED = 4;

    public String getStatusDesc() {
        switch(status) {
            case STATUS_PENDING: return "待支付";
            case STATUS_PAID: return "已支付";
            case STATUS_SHIPPED: return "已发货";
            case STATUS_COMPLETED: return "已完成";
            case STATUS_CANCELLED: return "已取消";
            default: return "未知";
        }
    }
}