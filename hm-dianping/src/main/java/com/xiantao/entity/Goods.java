package com.xiantao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_goods")
public class Goods implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品分类id
     */
    private Long typeId;

    /**
     * 商品图片，多个图片以','隔开
     */
    private String images;

    /**
     * 所在区域，例如大学城
     */
    private String area;

    /**
     * 交易地址（支持自提/当面交易）
     */
    private String address;

    /**
     * 经度
     */
    private Double x;

    /**
     * 维度
     */
    private Double y;

    /**
     * 售价（元）
     */
    private Long price;

    /**
     * 成交数量
     */
    private Integer sold;

    /**
     * 留言/想要数量
     */
    private Integer comments;

    /**
     * 卖家信用评分，1~5分，乘10保存，避免小数
     */
    private Integer score;

    /**
     * 成色描述，例如 95新、轻微使用痕迹
     * condition 是 MySQL 保留关键字，必须加反引号转义
     */
    @TableField("`condition`")
    private String condition;

    /**
     * 卖家用户id
     */
    private Long sellerId;

    /**
     * 商品状态，1：在售；2：已售；3：下架
     */
    private Integer status;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


    @TableField(exist = false)
    private Double distance;
}
