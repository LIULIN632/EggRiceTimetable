package com.eggrice.timetable.ui.treasurebox

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class LearningResource(
    val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val courseName: String,
    val blogger: String,
    val description: String,
    val videoUrl: String,
    val dayOfWeek: Int = 1,
    val startSlot: Int = 1,
    val endSlot: Int = 2,
    val isCustom: Boolean = false
)

@Immutable
data class FoodOption(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "",
    val windowName: String = "",
    val price: String = "",
    val isCustom: Boolean = false
)

/** Subjects for learning resources */
val SUBJECTS = listOf("数学", "编程", "英语", "考研", "设计", "通识", "自定义")

/** Default learning resources */
val DEFAULT_LEARNING_RESOURCES = listOf(
    LearningResource("lr1", "数学", "高等数学（上）", "宋浩老师", "B站最火高数课，讲课幽默风趣，零基础也能听懂", "https://b23.tv/gaoshu_songhao", 1, 1, 2),
    LearningResource("lr2", "数学", "线性代数", "3Blue1Brown", "用动画直观理解线代本质，全球公认最佳入门", "https://b23.tv/xiandaishu_3b1b", 2, 1, 2),
    LearningResource("lr3", "数学", "概率论与数理统计", "浙大苏德矿", "矿爷经典课程，讲解透彻，考研必备", "https://b23.tv/gailvlun_kuang", 3, 1, 2),
    LearningResource("lr4", "编程", "Java从入门到精通", "狂神说Java", "全网最全Java教程，配套笔记+源码，适合零基础", "https://b23.tv/java_kuangshen", 1, 3, 4),
    LearningResource("lr5", "编程", "Python爬虫实战", "崔庆才", "《Python3网络爬虫开发实战》作者亲授", "https://b23.tv/python_cui", 2, 3, 4),
    LearningResource("lr6", "编程", "数据结构与算法", "左程云", "大厂面试算法通关，LeetCode刷题路线", "https://b23.tv/suanfa_zuo", 3, 3, 4),
    LearningResource("lr7", "英语", "四六级急救班", "刘晓艳", "考前突击必备，作文模板+听力技巧", "https://b23.tv/cet46_liu", 4, 1, 2),
    LearningResource("lr8", "英语", "英语语法全程", "英语的平行世界", "用中文思维讲透英语语法，简单易懂", "https://b23.tv/yufa_parallel", 5, 1, 2),
    LearningResource("lr9", "考研", "考研政治", "徐涛", "强化班+冲刺班，每年数百万考生跟学", "https://b23.tv/kaoyanzhengzhi_xu", 1, 5, 6),
    LearningResource("lr10", "考研", "考研数学", "张宇", "《张宇36讲》配套视频，高数线代概率全覆盖", "https://b23.tv/kaoyanmath_zhang", 2, 5, 6),
    LearningResource("lr11", "设计", "PS教程2024", "李涛", "Photoshop零基础到精通，案例驱动教学", "https://b23.tv/ps_litao", 4, 3, 4),
    LearningResource("lr12", "通识", "中国哲学简史", "王德峰", "复旦大学哲学王子，讲透儒释道精髓", "https://b23.tv/zhexue_wang", 5, 7, 8)
)

/** Default food options */
val DEFAULT_FOOD_OPTIONS = listOf(
    FoodOption("f1", "黄焖鸡米饭", "食堂"),
    FoodOption("f2", "麻辣香锅", "食堂"),
    FoodOption("f3", "兰州拉面", "面食"),
    FoodOption("f4", "沙县小吃", "小吃"),
    FoodOption("f5", "麻辣烫", "食堂"),
    FoodOption("f6", "汉堡薯条套餐", "快餐"),
    FoodOption("f7", "烤鱼饭", "食堂"),
    FoodOption("f8", "鸡排饭", "食堂"),
    FoodOption("f9", "过桥米线", "面食"),
    FoodOption("f10", "蛋炒饭", "食堂"),
    FoodOption("f11", "水饺（韭菜鸡蛋）", "面食"),
    FoodOption("f12", "披萨", "快餐"),
    FoodOption("f13", "寿司拼盘", "外卖"),
    FoodOption("f14", "酸辣粉", "小吃"),
    FoodOption("f15", "炸鸡啤酒", "外卖"),
    FoodOption("f16", "煲仔饭", "外卖"),
    FoodOption("f17", "牛肉面", "面食"),
    FoodOption("f18", "煎饼果子", "小吃"),
    FoodOption("f19", "麻辣拌", "食堂"),
    FoodOption("f20", "糖醋里脊盖饭", "食堂"),
    FoodOption("f21", "螺蛳粉", "小吃"),
    FoodOption("f22", "烤肉拌饭", "外卖"),
    FoodOption("f23", "热干面", "面食"),
    FoodOption("f24", "馄饨/抄手", "面食"),
    FoodOption("f25", "咖喱鸡饭", "食堂")
)
