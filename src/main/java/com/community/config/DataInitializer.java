package com.community.config;

import com.community.domain.DispatchRecord;
import com.community.domain.ItemStatus;
import com.community.domain.OrderStatus;
import com.community.domain.OrderStatusLog;
import com.community.domain.Review;
import com.community.domain.Role;
import com.community.domain.ServiceCategory;
import com.community.domain.ServiceItem;
import com.community.domain.ServiceOrder;
import com.community.domain.StaffProfile;
import com.community.domain.StaffStatus;
import com.community.domain.User;
import com.community.repository.DispatchRecordRepository;
import com.community.repository.OrderStatusLogRepository;
import com.community.repository.ReviewRepository;
import com.community.repository.ServiceCategoryRepository;
import com.community.repository.ServiceItemRepository;
import com.community.repository.ServiceOrderRepository;
import com.community.repository.StaffProfileRepository;
import com.community.repository.UserRepository;
import com.community.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 首次启动初始化演示数据：账号、服务分类、服务项目、历史订单与评价。
 * <p>数据写入 data/*.json，重启不会丢失；只要 users.json 已有内容就跳过，避免重复灌数。</p>
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceItemRepository itemRepository;
    private final ServiceOrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final CatalogService catalogService;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("检测到已有业务数据，跳过初始化。数据目录：{}", dataHint());
            warmUpCache();
            return;
        }
        log.info("首次启动，开始初始化演示数据 ...");

        User admin = createUser("admin", "admin123", "王建国", "13800000001",
                "admin@community.example.com", Role.ADMIN, "社区服务中心");
        User staff1 = createUser("staff01", "staff123", "张秀兰", "13800000101",
                "zhang@community.example.com", Role.STAFF, null);
        User staff2 = createUser("staff02", "staff123", "李国强", "13800000102",
                "li@community.example.com", Role.STAFF, null);
        User staff3 = createUser("staff03", "staff123", "王铁柱", "13800000103",
                "wang@community.example.com", Role.STAFF, null);
        User staff4 = createUser("staff04", "staff123", "赵春梅", "13800000104",
                "zhao@community.example.com", Role.STAFF, null);

        createStaffProfile(staff1, "保洁,深度清洁,玻璃清洁,开荒保洁", LocalTime.of(8, 0), LocalTime.of(18, 0),
                6, 6, "从业 6 年，擅长深度保洁与玻璃清洁，服务过多家社区。");
        createStaffProfile(staff2, "家电维修,空调维修,洗衣机维修,冰箱维修", LocalTime.of(9, 0),
                LocalTime.of(19, 0), 5, 8, "家电维修老师傅，持证上岗，擅长空调与洗衣机故障排查。");
        createStaffProfile(staff3, "开锁,换锁,门锁维修,智能锁安装", LocalTime.of(7, 0), LocalTime.of(22, 0),
                8, 10, "公安备案开锁师傅，24 小时应急响应。");
        createStaffProfile(staff4, "快递代收,代取快递,代跑腿,搬运", LocalTime.of(8, 0), LocalTime.of(20, 0),
                10, 3, "熟悉小区快递柜与驿站，代取代送均可。");

        User resident1 = createUser("resident01", "resident123", "陈静", "13800000201",
                "chen@example.com", Role.RESIDENT, "阳光小区 3 号楼 2 单元 501");
        User resident2 = createUser("resident02", "resident123", "刘洋", "13800000202",
                "liu@example.com", Role.RESIDENT, "阳光小区 5 号楼 1 单元 302");
        User resident3 = createUser("resident03", "resident123", "周敏", "13800000203",
                "zhou@example.com", Role.RESIDENT, "阳光小区 7 号楼 2 单元 1102");

        Map<String, ServiceCategory> categories = seedCategories();
        List<ServiceItem> items = seedItems(categories);

        seedOrders(List.of(resident1, resident2, resident3), items, admin,
                List.of(staff1, staff2, staff3, staff4));

        warmUpCache();
        log.info("演示数据初始化完成。账号：admin/admin123、staff01/staff123、resident01/resident123");
    }

    private String dataHint() {
        return "data/*.json";
    }

    private User createUser(String username, String rawPassword, String realName, String phone,
                            String email, Role role, String address) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(role);
        user.setAddress(address);
        user.setEnabled(true);
        user.touch();
        return userRepository.save(user);
    }

    private void createStaffProfile(User user, String skills, LocalTime start, LocalTime end,
                                    int maxDaily, int years, String intro) {
        StaffProfile profile = new StaffProfile();
        profile.setUserId(user.getId());
        profile.setSkills(skills);
        profile.setWorkStart(start);
        profile.setWorkEnd(end);
        profile.setMaxDailyOrders(maxDaily);
        profile.setServiceYears(years);
        profile.setIntro(intro);
        profile.setStatus(StaffStatus.FREE);
        profile.touch();
        staffProfileRepository.save(profile);
    }

    private Map<String, ServiceCategory> seedCategories() {
        Map<String, ServiceCategory> map = new HashMap<>();
        map.put("clean", category("clean", "家政保洁", "🧹", "日常保洁、深度清洁、开荒保洁，还您一个干净家", 1));
        map.put("repair", category("repair", "家电维修", "🔧", "空调、洗衣机、冰箱等家电上门检修", 2));
        map.put("express", category("express", "快递代收", "📦", "代取快递、送货上门、大件搬运", 3));
        map.put("lock", category("lock", "上门开锁", "🔑", "公安备案开锁师傅，急事急办", 4));
        map.put("pipe", category("pipe", "管道疏通", "🚿", "厨房下水、马桶、地漏疏通", 5));
        map.put("errand", category("errand", "生活跑腿", "🛵", "代购、代缴、陪同办事等生活小事", 6));
        return map;
    }

    private ServiceCategory category(String code, String name, String icon, String desc, int sort) {
        ServiceCategory category = new ServiceCategory();
        category.setCode(code);
        category.setName(name);
        category.setIcon(icon);
        category.setDescription(desc);
        category.setSortOrder(sort);
        category.setEnabled(true);
        category.touch();
        return categoryRepository.save(category);
    }

    private List<ServiceItem> seedItems(Map<String, ServiceCategory> categories) {
        List<ServiceItem> items = new ArrayList<>();
        items.add(item(categories.get("clean"), "日常保洁（2 小时）", "客厅卧室厨卫基础清洁",
                "服务内容：地面清扫拖洗、桌面除尘、卫生间与厨房台面清洁、垃圾清运。\n"
                        + "服务说明：自带基础清洁工具与耗材；不含高空外墙、家电内部拆洗。\n"
                        + "计价方式：按次计费，超出 2 小时按 40 元/小时加收。",
                new BigDecimal("98.00"), "次", 120, true, 168, 42, 196));
        items.add(item(categories.get("clean"), "深度保洁（4 小时）", "厨房油污 + 卫生间死角深度处理",
                "服务内容：厨房重油污处理、卫生间除垢消毒、门窗轨道清洁、家具表面养护。\n"
                        + "服务说明：适合季度大扫除，需提前 1 天预约；不含外墙玻璃。",
                new BigDecimal("268.00"), "次", 240, true, 96, 21, 99));
        items.add(item(categories.get("clean"), "玻璃清洁（全屋）", "内外窗玻璃 + 纱窗清洗",
                "服务内容：全屋窗户玻璃双面清洁、纱窗拆洗、窗框擦拭。\n"
                        + "服务说明：高层外窗需物业配合，雨天顺延。",
                new BigDecimal("158.00"), "次", 150, false, 54, 12, 57));
        items.add(item(categories.get("clean"), "开荒保洁", "新房入住前整体清洁",
                "服务内容：装修粉尘清理、胶渍去除、全屋深度保洁。\n服务说明：按建筑面积计价，80㎡ 以内一口价。",
                new BigDecimal("480.00"), "次", 360, false, 23, 5, 24));

        items.add(item(categories.get("repair"), "空调清洗保养", "挂机/柜机深度清洗",
                "服务内容：滤网清洗、蒸发器清洗、外壳消毒、制冷效果检测。\n服务说明：不含加氟，加氟另计 80 元/压。",
                new BigDecimal("120.00"), "台", 90, true, 142, 35, 166));
        items.add(item(categories.get("repair"), "洗衣机维修", "不排水、不脱水、异响检修",
                "服务内容：上门检测、故障判断、常见配件更换。\n服务说明：上门费 30 元，维修成功后免上门费；配件另计。",
                new BigDecimal("80.00"), "次", 60, true, 87, 19, 90));
        items.add(item(categories.get("repair"), "冰箱不制冷检修", "压缩机、温控、管路检测",
                "服务内容：上门检测制冷系统、温控器与压缩机工况。\n服务说明：需提前清空冷藏冷冻室。",
                new BigDecimal("100.00"), "次", 60, false, 41, 9, 43));
        items.add(item(categories.get("repair"), "热水器保养", "除垢 + 安全检查",
                "服务内容：内胆除垢、镁棒检查、燃气/电路安全检查。\n服务说明：建议每 12 个月一次。",
                new BigDecimal("150.00"), "台", 90, false, 35, 8, 38));

        items.add(item(categories.get("express"), "快递代取（小件）", "驿站代取送上楼",
                "服务内容：凭取件码到驿站代取，30 分钟内送达家门口。\n服务说明：单件 5kg 以内，超重另议。",
                new BigDecimal("6.00"), "件", 30, true, 326, 68, 331));
        items.add(item(categories.get("express"), "大件搬运上门", "米面粮油、家电上楼",
                "服务内容：2 人协助搬运，含上楼。\n服务说明：无电梯 4 层以上每层加收 10 元。",
                new BigDecimal("40.00"), "次", 45, false, 118, 24, 121));
        items.add(item(categories.get("express"), "快递代寄", "上门取件 + 打包",
                "服务内容：上门取件、简易打包、代填面单。\n服务说明：运费按快递公司实际报价。",
                new BigDecimal("10.00"), "次", 30, false, 76, 15, 78));

        items.add(item(categories.get("lock"), "上门开锁（防盗门）", "公安备案，10 分钟响应",
                "服务内容：防盗门应急开锁，不破坏门体。\n服务说明：需出示身份证与房产证明；夜间 22:00 后加收 30 元。",
                new BigDecimal("80.00"), "次", 30, true, 64, 16, 71));
        items.add(item(categories.get("lock"), "换锁芯（C 级）", "含 C 级锁芯与安装",
                "服务内容：拆除旧锁芯、安装 C 级锁芯、调试。\n服务说明：锁芯质保 2 年。",
                new BigDecimal("220.00"), "次", 45, false, 38, 9, 42));
        items.add(item(categories.get("lock"), "智能锁安装调试", "指纹锁 / 密码锁",
                "服务内容：旧锁拆除、智能锁安装、指纹与密码录入教学。\n服务说明：不含锁具本身费用。",
                new BigDecimal("180.00"), "次", 90, false, 27, 6, 29));

        items.add(item(categories.get("pipe"), "厨房下水疏通", "油污堵塞处理",
                "服务内容：管道疏通、油污清理、通水测试。\n服务说明：主管道堵塞需物业协同。",
                new BigDecimal("90.00"), "次", 45, true, 73, 17, 82));
        items.add(item(categories.get("pipe"), "马桶疏通", "异物 / 结垢堵塞",
                "服务内容：专业工具疏通、异味处理。\n服务说明：瓷体破损不在服务范围。",
                new BigDecimal("70.00"), "次", 30, false, 58, 13, 65));

        items.add(item(categories.get("errand"), "代买菜（1 小时内）", "按清单代购送到家",
                "服务内容：按清单到附近商超代购并送达。\n服务说明：商品费用实报实销，代购费按单收取。",
                new BigDecimal("15.00"), "次", 60, false, 42, 10, 47));
        items.add(item(categories.get("errand"), "陪同就医", "半天陪诊服务",
                "服务内容：陪同挂号、取药、检查引导。\n服务说明：半天 4 小时，超时按 30 元/小时。",
                new BigDecimal("120.00"), "次", 240, false, 19, 5, 24));

        return items;
    }

    private ServiceItem item(ServiceCategory category, String name, String subtitle, String description,
                             BigDecimal price, String unit, int duration, boolean hot,
                             int sales, int reviewCount, int scoreTotal) {
        ServiceItem item = new ServiceItem();
        item.setCategoryId(category.getId());
        item.setName(name);
        item.setSubtitle(subtitle);
        item.setDescription(description);
        item.setPrice(price);
        item.setUnit(unit);
        item.setDurationMinutes(duration);
        item.setHot(hot);
        item.setStatus(ItemStatus.ON_SALE);
        item.setSales(sales);
        item.setReviewCount(reviewCount);
        item.setScoreTotal(scoreTotal);
        item.touch();
        return itemRepository.save(item);
    }

    private void seedOrders(List<User> residents, List<ServiceItem> items, User admin, List<User> staffs) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<Long, User> staffBySkill = new HashMap<>();
        staffBySkill.put(items.get(0).getCategoryId(), staffs.get(0));
        staffBySkill.put(items.get(4).getCategoryId(), staffs.get(1));
        staffBySkill.put(items.get(11).getCategoryId(), staffs.get(2));
        staffBySkill.put(items.get(8).getCategoryId(), staffs.get(3));

        List<Review> reviews = new ArrayList<>();
        List<StaffProfile> profiles = staffProfileRepository.findAll();

        for (int i = 0; i < 28; i++) {
            ServiceItem item = items.get(random.nextInt(items.size()));
            User resident = residents.get(random.nextInt(residents.size()));
            // 固定分布，保证演示数据覆盖全部状态：
            // 今日 4 单（待接单 / 已派单 / 服务中 / 已取消），昨日 4 单（已派单 / 已完成），其余为历史已完成单
            int daysAgo = i < 4 ? 0 : (i < 8 ? 1 : 2 + (i % 10));
            OrderStatus status;
            if (i < 4) {
                status = switch (i) {
                    case 0 -> OrderStatus.PENDING;
                    case 1 -> OrderStatus.ASSIGNED;
                    case 2 -> OrderStatus.IN_SERVICE;
                    default -> OrderStatus.CANCELLED;
                };
            } else if (i < 8) {
                status = i % 2 == 0 ? OrderStatus.ASSIGNED : OrderStatus.COMPLETED;
            } else {
                status = OrderStatus.COMPLETED;
            }
            LocalDateTime created = LocalDateTime.now().minusDays(daysAgo)
                    .withHour(8 + (i * 3) % 10).withMinute((i * 7) % 60);

            ServiceOrder order = new ServiceOrder();
            order.setOrderNo("CS" + created.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + String.format("%03d", i));
            order.setRequestToken("seed-token-" + i);
            order.setResidentId(resident.getId());
            order.setServiceItemId(item.getId());
            order.setServiceItemName(item.getName());
            order.setCategoryName(categoryRepository.findById(item.getCategoryId())
                    .map(ServiceCategory::getName).orElse("其他"));
            order.setPrice(item.getPrice());
            order.setQuantity(1);
            order.setTotalAmount(item.getPrice());
            order.setAddress(resident.getAddress());
            order.setContactName(resident.getRealName());
            order.setContactPhone(resident.getPhone());
            order.setRemark(i % 5 == 0 ? "家里有老人，请提前电话联系" : null);
            order.setStatus(status);
            order.setCreatedAt(created);
            order.setUpdatedAt(created);

            User staff = staffBySkill.getOrDefault(item.getCategoryId(),
                    staffs.get(random.nextInt(staffs.size())));
            if (status != OrderStatus.PENDING && status != OrderStatus.CANCELLED) {
                order.setStaffId(staff.getId());
                order.setAssignedAt(created.plusMinutes(20));
            }
            if (status == OrderStatus.IN_SERVICE || status == OrderStatus.COMPLETED) {
                order.setStartedAt(created.plusHours(1));
            }
            if (status == OrderStatus.COMPLETED) {
                order.setCompletedAt(created.plusHours(2));
                order.setAppointmentTime(created.plusHours(1));
            } else if (status == OrderStatus.IN_SERVICE) {
                order.setAppointmentTime(created.plusMinutes(30));
            } else {
                order.setAppointmentTime(created.plusDays(1).withHour(9 + random.nextInt(8)).withMinute(0));
            }
            if (status == OrderStatus.CANCELLED) {
                order.setCancelledAt(created.plusMinutes(45));
                order.setCancelReason(random.nextBoolean() ? "居民主动取消" : "超时未派单，系统自动取消");
            }
            ServiceOrder saved = orderRepository.save(order);

            statusLogRepository.save(OrderStatusLog.of(saved, null, OrderStatus.PENDING, "居民", "提交预约单"));
            if (order.getStaffId() != null) {
                DispatchRecord record = new DispatchRecord();
                record.setOrderId(saved.getId());
                record.setStaffId(order.getStaffId());
                record.setStaffName(staff.getRealName());
                record.setType(random.nextBoolean() ? "AUTO" : "MANUAL");
                record.setMatchScore(80 + random.nextInt(20) + 0.0);
                record.setReason("技能匹配：" + item.getName());
                record.setCreatedAt(created.plusMinutes(20));
                dispatchRecordRepository.save(record);
                statusLogRepository.save(OrderStatusLog.of(saved, OrderStatus.PENDING, OrderStatus.ASSIGNED,
                        "系统管理员", "派单给 " + staff.getRealName()));
            }
            if (status == OrderStatus.IN_SERVICE) {
                statusLogRepository.save(OrderStatusLog.of(saved, OrderStatus.ASSIGNED, OrderStatus.IN_SERVICE,
                        "服务人员", "开始上门服务"));
            }
            if (status == OrderStatus.COMPLETED) {
                statusLogRepository.save(OrderStatusLog.of(saved, OrderStatus.ASSIGNED, OrderStatus.IN_SERVICE,
                        "服务人员", "开始上门服务"));
                statusLogRepository.save(OrderStatusLog.of(saved, OrderStatus.IN_SERVICE, OrderStatus.COMPLETED,
                        "服务人员", "服务完成"));
                if (random.nextInt(10) < 7) {
                    Review review = new Review();
                    review.setOrderId(saved.getId());
                    review.setOrderNo(saved.getOrderNo());
                    review.setResidentId(resident.getId());
                    review.setStaffId(order.getStaffId());
                    review.setItemId(item.getId());
                    review.setScore(random.nextInt(10) < 8 ? 5 : 4);
                    review.setTags(random.nextBoolean() ? "准时,专业" : "态度好,干净整洁");
                    review.setContent(random.nextBoolean() ? "师傅很准时，活干得细致，下次还找他。"
                            : "上门速度快，收费透明，服务态度好。");
                    review.setCreatedAt(created.plusHours(3));
                    reviews.add(reviewRepository.save(review));
                    saved.setReviewed(true);
                    orderRepository.save(saved);
                }
            }
        }

        // 依据真实评价回写服务人员评分
        for (StaffProfile profile : profiles) {
            double avg = reviewRepository.avgScoreByStaff(profile.getUserId());
            long count = reviewRepository.countByStaffId(profile.getUserId());
            profile.setAvgScore(count == 0 ? 5.0 : Math.round(avg * 10) / 10.0);
            profile.setCompletedCount((int) orderRepository.countByStaffIdAndStatus(profile.getUserId(),
                    OrderStatus.COMPLETED));
            staffProfileRepository.save(profile);
        }
        log.info("初始化订单 {} 单，评价 {} 条", orderRepository.count(), reviews.size());
    }

    private void warmUpCache() {
        try {
            catalogService.warmUp();
        } catch (Exception e) {
            log.warn("缓存预热失败（Redis 可能未启动），功能不受影响：{}", e.getMessage());
        }
    }
}
