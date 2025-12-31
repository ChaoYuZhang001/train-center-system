package com.train.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.train.annotation.IgnoreLog;
import com.train.entity.*;
import com.train.enums.OperateMenu;
import com.train.enums.OperateType;
import com.train.mapper.*;
import com.train.security.JwtUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 操作日志切面（记录新增/编辑/删除/查询操作）
 * 职责：统一拦截系统操作，记录标准化操作日志，包含敏感信息脱敏，提取@Operation注解summary优化operContent
 * 核心特性：仅编辑操作保留原数据+新数据嵌套结构，非编辑操作简化数据结构
 */
@Aspect
@Component
public class SysOperLogAspect {

    // ==================== 常量定义（集中管理，消除魔法值） ====================
    private static final Logger log = LoggerFactory.getLogger(SysOperLogAspect.class);
    // IP地址正则表达式（IPv4）
    private static final Pattern IP_V4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    // 敏感字段正则表达式 - 扩展更多敏感字段（大小写不敏感）
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(password|pwd|secret|token|key|card|idcard|phone|mobile|email|tel)", Pattern.CASE_INSENSITIVE
    );
    // ID格式验证正则表达式（字母、数字、下划线、短横线，1-64位）
    private static final Pattern ID_FORMAT_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    // 认证相关URI（无需记录日志）
    private static final Set<String> AUTH_URI_SET = new HashSet<>(Arrays.asList("/auth/login", "/auth/logout"));
    // 日志ID格式（9位时间戳 + 3位随机数）
    private static final String LOG_ID_FORMAT = "%09d%03d";
    // 默认操作内容（编辑操作专用：嵌套结构）
    private static final String DEFAULT_EDIT_CONTENT = "{\"原数据\":{},\"新数据\":{}}";
    // 简易默认内容（非编辑操作专用：无嵌套）
    private static final String DEFAULT_SIMPLE_CONTENT = "{}";
    // 操作失败内容前缀
    private static final String OPER_FAILED_PREFIX = "操作失败：";
    // 反射相关常量
    private static final String SELECT_BY_ID_METHOD = "selectById";
    private static final String EMPTY_STR = "";
    // @Operation注解summary拼接分隔符
    private static final String SUMMARY_SEPARATOR = "：";

    // ==================== 依赖注入（按需注入，避免冗余） ====================
    @Resource
    private SysOperLogMapper sysOperLogMapper;
    @Resource
    private SysOrgMapper sysOrgMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private TrainQuestionMapper trainQuestionMapper;
    @Resource
    private TrainAnswerMapper trainAnswerMapper;
    @Resource
    private TrainVideoMapper trainVideoMapper;
    @Resource
    private TrainQualityMapper trainQualityMapper;
    @Resource
    private ObjectMapper objectMapper;

    // ==================== 反射配置（核心：实体与字段、Mapper的映射，消除硬编码） ====================
    /**
     * 实体配置映射：key=实体Class，value=实体配置（主键字段名、业务字段列表、对应Mapper）
     * 新增实体时，仅需添加配置，无需修改业务逻辑
     */
    private final Map<Class<?>, EntityConfig> entityConfigMap = new HashMap<>(16); // 初始化容量，提升性能

    /**
     * 实体配置内部类（存储主键字段、业务字段、对应Mapper）
     */
    private static class EntityConfig {
        private final String idField; // 主键字段名（如：orgId、userId）
        private final List<String> bizFields; // 业务字段列表（如：orgName、userName）
        private final Object mapper; // 对应Mapper实例（如：sysOrgMapper、sysUserMapper）

        public EntityConfig(String idField, List<String> bizFields, Object mapper) {
            this.idField = idField;
            this.bizFields = Collections.unmodifiableList(bizFields); // 不可变列表，防止外部修改
            this.mapper = mapper;
        }

        // Getter方法
        public String getIdField() { return idField; }
        public List<String> getBizFields() { return bizFields; }
        public Object getMapper() { return mapper; }
    }

    /**
     * 初始化实体配置（项目启动时执行，仅初始化一次）
     * 新增实体时，直接在此添加配置即可，无需修改其他方法
     */
    @PostConstruct
    private void initEntityConfig() {
        // 1. 配置 SysOrg 实体
        entityConfigMap.put(SysOrg.class, new EntityConfig(
                "orgId",
                new ArrayList<>(Arrays.asList("orgId", "orgName")),
                sysOrgMapper
        ));

        // 2. 配置 SysUser 实体
        entityConfigMap.put(SysUser.class, new EntityConfig(
                "userId",
                new ArrayList<>(Arrays.asList("userId", "userName", "account", "status")),
                sysUserMapper
        ));

        // 3. 配置 SysRole 实体
        entityConfigMap.put(SysRole.class, new EntityConfig(
                "roleId",
                new ArrayList<>(Arrays.asList("roleId", "roleName", "roleDesc")),
                sysRoleMapper
        ));

        // 4. 配置 TrainQuality 实体
        entityConfigMap.put(TrainQuality.class, new EntityConfig(
                "qualityId",
                new ArrayList<>(Arrays.asList("qualityId", "userId", "score")),
                trainQualityMapper
        ));
        // 5. 配置 TrainVideo 实体
        entityConfigMap.put(TrainVideo.class, new EntityConfig(
                "videoId",
                new ArrayList<>(Arrays.asList("videoId", "videoName", "coverUrl", "playUrl")),
                trainVideoMapper
        ));
        // 6. 配置 TrainAnswer 实体
        entityConfigMap.put(TrainAnswer.class, new EntityConfig(
                "answerId",
                new ArrayList<>(Arrays.asList("answerId", "userId", "paperId", "questionId", "userAnswer", "score")),
                trainAnswerMapper
        ));
        // 7. 配置 TrainQuestion 实体
        entityConfigMap.put(TrainQuestion.class, new EntityConfig(
                "questionId",
                new ArrayList<>(Arrays.asList("questionId", "questionName", "questionType", "questionScore")),
                trainQuestionMapper
        ));
    }

    /**
     * 定义切入点
     * 拦截范围：
     * 1. com.train.controller 包下所有方法
     * 2. 排除 @IgnoreLog 注解标记的方法
     * 3. 排除 AuthController 类
     * 4. 仅拦截 POST/PUT/DELETE/GET 请求方法
     */
    @Pointcut("execution(* com.train.controller..*(..)) " +
            "&& !@annotation(com.train.annotation.IgnoreLog) " +
            "&& !within(com.train.controller.AuthController) " +
            "&& (@annotation(org.springframework.web.bind.annotation.PostMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.GetMapping))")
    public void operLogPointcut() {}

    /**
     * 后置通知：接口执行成功后记录日志
     */
    @AfterReturning(pointcut = "operLogPointcut()", returning = "result")
    public void recordOperLog(JoinPoint joinPoint, Object result) {
        recordOperationLog(joinPoint, "成功", null, result);
    }

    /**
     * 异常通知：接口执行失败后记录日志
     */
    @AfterThrowing(pointcut = "operLogPointcut()", throwing = "ex")
    public void recordFailedOperLog(JoinPoint joinPoint, Exception ex) {
        recordOperationLog(joinPoint, "失败", ex, null);
    }

    /**
     * 统一记录操作日志的核心方法（提取公共逻辑，消除冗余）
     * @param joinPoint 切点信息
     * @param operResult 操作结果（成功/失败）
     * @param ex 异常对象（失败时非空）
     * @param result 接口返回结果（成功时非空）
     */
    private void recordOperationLog(JoinPoint joinPoint, String operResult, Exception ex, Object result) {
        // 1. 获取请求上下文，空值快速返回
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("请求上下文为空，跳过操作日志记录");
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String requestUri = Optional.ofNullable(request.getRequestURI()).orElse(EMPTY_STR);

        // 2. 过滤认证相关请求，无需记录日志
        if (AUTH_URI_SET.stream().anyMatch(requestUri::contains)) {
            return;
        }

        // 3. 获取认证信息，未认证则跳过
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof JwtUserDetails)) {
            log.warn("用户未认证或认证信息异常，跳过操作日志记录");
            return;
        }

        try {
            // 4. 提取核心信息（操作人、请求、接口、参数等）
            JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal();
            String operName = extractOperName(userDetails);
            String operIp = getClientIpAddress(request);
            String requestMethod = Optional.ofNullable(request.getMethod()).orElse(EMPTY_STR);
            String operMenu = getModuleByUri(requestUri);
            String operType = getOperTypeByMethod(requestMethod);
            Object[] args = joinPoint.getArgs();

            // ===== 关键优化：获取@Operation注解的summary值 =====
            String operationSummary = getOperationSummary(joinPoint);
            // 构建operContent（核心修改：仅编辑操作保留嵌套结构）
            String operContent = buildOperContent(operationSummary, args, result, operType, operResult, ex);

            String targetId = getOperObjId(args, requestUri);
            String targetName = getOperObjName(args);

            // 5. 构建并保存日志实体
            SysOperLog operLog = buildSysOperLog(userDetails, operName, operIp, operMenu, operType,
                    targetId, targetName, operContent, operResult);
            sysOperLogMapper.insert(operLog);

        } catch (Exception e) {
            // 日志记录失败不影响主业务流程，仅打印错误日志
            log.error("操作日志记录失败", e);
        }
    }

    /**
     * 提取方法上@Operation注解的summary值
     * @param joinPoint 切点信息
     * @return summary值（注解不存在时返回空字符串）
     */
    private String getOperationSummary(JoinPoint joinPoint) {
        try {
            // 1. 获取方法签名
            Signature signature = joinPoint.getSignature();
            if (!(signature instanceof MethodSignature)) {
                log.warn("切点签名非方法签名，无法获取@Operation注解");
                return EMPTY_STR;
            }
            MethodSignature methodSignature = (MethodSignature) signature;

            // 2. 获取目标方法
            Class<?> targetClass = joinPoint.getTarget().getClass();
            Method targetMethod = targetClass.getMethod(
                    methodSignature.getName(),
                    methodSignature.getParameterTypes()
            );

            // 3. 获取@Operation注解并提取summary
            if (targetMethod.isAnnotationPresent(Operation.class)) {
                Operation operation = targetMethod.getAnnotation(Operation.class);
                return Optional.ofNullable(operation.summary()).orElse(EMPTY_STR);
            }
        } catch (NoSuchMethodException e) {
            log.error("获取目标方法失败，无法提取@Operation注解", e);
        } catch (Exception e) {
            log.error("提取@Operation注解summary失败", e);
        }
        return EMPTY_STR;
    }

    /**
     * 构建操作内容JSON（核心修改：仅编辑操作保留原数据+新数据嵌套结构）
     */
    private String buildOperContent(String operationSummary, Object[] args, Object result,
                                    String operType, String operResult, Exception ex) {
        // 1. 操作失败场景：直接拼接summary与异常信息，无需数据结构
        if ("失败".equals(operResult) && ex != null) {
            String errorMsg = OPER_FAILED_PREFIX + sanitizeErrorMessage(ex.getMessage());
            // 有summary则拼接，无则直接返回错误信息
            if (StringUtils.hasText(operationSummary)) {
                return operationSummary + SUMMARY_SEPARATOR + errorMsg;
            } else {
                return errorMsg;
            }
        }

        // 2. 操作成功场景：初始化数据容器
        Map<String, Object> oldData = new HashMap<>(8);
        Map<String, Object> newData = new HashMap<>(8);
        String coreJson; // 核心数据序列化结果

        // 3. 根据操作类型提取数据，并构建对应数据结构
        if (OperateType.EDIT.getDesc().equals(operType)) {
            // ===== 仅编辑操作：构建原数据+新数据嵌套结构 =====
            extractOldAndNewDataByReflection(args, oldData, newData);
            Map<String, Object> contentMap = new HashMap<>(2);
            contentMap.put("原数据", oldData);
            contentMap.put("新数据", newData);
            // 序列化嵌套结构
            try {
                coreJson = objectMapper.writeValueAsString(contentMap);
            } catch (JsonProcessingException e) {
                log.error("序列化编辑操作数据失败", e);
                coreJson = DEFAULT_EDIT_CONTENT;
            }
        } else if (OperateType.ADD.getDesc().equals(operType)) {
            // 新增操作：仅序列化新数据（无嵌套）
            extractDataByReflection(args, newData, false);
            try {
                coreJson = objectMapper.writeValueAsString(newData);
            } catch (JsonProcessingException e) {
                log.error("序列化新增操作数据失败", e);
                coreJson = DEFAULT_SIMPLE_CONTENT;
            }
        } else if (OperateType.DELETE.getDesc().equals(operType)) {
            // 删除操作：仅序列化旧数据（无嵌套）
            extractDataByReflection(args, oldData, true);
            try {
                coreJson = objectMapper.writeValueAsString(oldData);
            } catch (JsonProcessingException e) {
                log.error("序列化删除操作数据失败", e);
                coreJson = DEFAULT_SIMPLE_CONTENT;
            }
        } else {
            // 查询操作：直接返回简易空对象（无嵌套）
            coreJson = DEFAULT_SIMPLE_CONTENT;
        }

        // 4. 敏感信息过滤
        coreJson = filterSensitiveInfo(coreJson);

        // 5. 融合summary与核心数据内容
        if (StringUtils.hasText(operationSummary)) {
            return operationSummary + SUMMARY_SEPARATOR + coreJson;
        } else {
            return coreJson;
        }
    }

    // ==================== 以下为原有核心方法（无修改，完全兼容新逻辑） ====================
    /**
     * 提取操作人姓名（处理@分隔符，简化逻辑，空值安全）
     */
    private String extractOperName(JwtUserDetails userDetails) {
        if (userDetails == null) {
            return EMPTY_STR;
        }
        String operName = userDetails.getUsername();
        if (StringUtils.hasText(operName) && operName.contains("@")) {
            String[] nameArray = operName.split("@");
            return nameArray.length > 0 ? nameArray[0] : operName;
        }
        return Optional.ofNullable(operName).orElse(EMPTY_STR);
    }

    /**
     * 获取客户端真实IP地址（优化逻辑，更简洁高效，空值安全）
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        List<String> ipHeaders = Arrays.asList("X-Forwarded-For", "X-Real-IP");
        for (String header : ipHeaders) {
            String ipStr = request.getHeader(header);
            if (StringUtils.hasText(ipStr) && !"unknown".equalsIgnoreCase(ipStr.trim())) {
                String[] ips = ipStr.split(",");
                for (String ip : ips) {
                    ip = ip.trim();
                    if (!"unknown".equalsIgnoreCase(ip) && isValidIpAddress(ip)) {
                        return ip;
                    }
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return isValidIpAddress(remoteAddr) ? remoteAddr : "0.0.0.0";
    }

    /**
     * 验证IP地址是否有效（仅IPv4，空值安全）
     */
    private boolean isValidIpAddress(String ip) {
        return StringUtils.hasText(ip) && IP_V4_PATTERN.matcher(ip).matches();
    }

    /**
     * 根据URI获取模块名称（使用枚举，消除硬编码，空值安全）
     */
    private String getModuleByUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            return OperateMenu.LOGIN_LOGOUT.getDesc();
        }
        if (uri.contains("/training/")) {
            return OperateMenu.TRAIN_MANAGEMENT.getDesc();
        } else if (uri.contains("/org/")) {
            return OperateMenu.ORG_MANAGEMENT.getDesc();
        } else if (uri.contains("/role/")) {
            return OperateMenu.ROLE_MANAGEMENT.getDesc();
        } else if (uri.contains("/user/")) {
            return OperateMenu.USER_MANAGEMENT.getDesc();
        } else {
            return OperateMenu.LOGIN_LOGOUT.getDesc();
        }
    }

    /**
     * 根据请求方法获取操作类型（使用枚举，消除硬编码，空值安全）
     */
    private String getOperTypeByMethod(String method) {
        if (!StringUtils.hasText(method)) {
            return OperateType.QUERY.getDesc();
        }
        switch (method.toUpperCase()) {
            case "POST":
                return OperateType.ADD.getDesc();
            case "PUT":
                return OperateType.EDIT.getDesc();
            case "DELETE":
                return OperateType.DELETE.getDesc();
            default:
                return OperateType.QUERY.getDesc();
        }
    }

    /**
     * 通用反射提取数据（支持新增/删除操作，统一逻辑）
     */
    private void extractDataByReflection(Object[] args, Map<String, Object> targetMap, boolean isOldData) {
        if (args == null || targetMap == null) {
            return;
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            Class<?> entityClass = arg.getClass();
            if (!entityConfigMap.containsKey(entityClass)) {
                continue;
            }

            EntityConfig entityConfig = entityConfigMap.get(entityClass);
            List<String> bizFields = entityConfig.getBizFields();
            Object mapper = entityConfig.getMapper();
            String idFieldName = entityConfig.getIdField();

            try {
                if (!isOldData) {
                    extractBizFieldsByReflection(arg, bizFields, targetMap);
                } else {
                    Object idValue = getFieldValueByReflection(arg, idFieldName);
                    if (idValue == null || !StringUtils.hasText(idValue.toString())) {
                        log.warn("实体{}主键字段{}值为空，跳过旧数据查询", entityClass.getSimpleName(), idFieldName);
                        continue;
                    }
                    Object oldEntity = queryOldEntityByReflection(mapper, idValue);
                    if (oldEntity != null) {
                        extractBizFieldsByReflection(oldEntity, bizFields, targetMap);
                    }
                }
            } catch (Exception e) {
                log.error("反射提取实体{}数据失败", entityClass.getSimpleName(), e);
                continue;
            }
        }
    }

    /**
     * 反射提取编辑操作的旧数据和新数据（复用通用逻辑，消除冗余）
     */
    private void extractOldAndNewDataByReflection(Object[] args, Map<String, Object> oldData, Map<String, Object> newData) {
        if (args == null || oldData == null || newData == null) {
            return;
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            Class<?> entityClass = arg.getClass();
            if (!entityConfigMap.containsKey(entityClass)) {
                continue;
            }

            EntityConfig entityConfig = entityConfigMap.get(entityClass);
            String idFieldName = entityConfig.getIdField();
            List<String> bizFields = entityConfig.getBizFields();
            Object mapper = entityConfig.getMapper();

            try {
                extractBizFieldsByReflection(arg, bizFields, newData);
                Object idValue = getFieldValueByReflection(arg, idFieldName);
                if (idValue == null || !StringUtils.hasText(idValue.toString())) {
                    log.warn("实体{}主键字段{}值为空，跳过旧数据查询", entityClass.getSimpleName(), idFieldName);
                    continue;
                }
                Object oldEntity = queryOldEntityByReflection(mapper, idValue);
                if (oldEntity != null) {
                    extractBizFieldsByReflection(oldEntity, bizFields, oldData);
                }
            } catch (Exception e) {
                log.error("反射处理编辑操作实体{}失败", entityClass.getSimpleName(), e);
                continue;
            }
        }
    }

    /**
     * 敏感信息过滤（优化递归逻辑，提高性能，空值安全）
     */
    private String filterSensitiveInfo(String json) {
        if (!StringUtils.hasText(json)) {
            return DEFAULT_SIMPLE_CONTENT; // 非编辑操作默认简易值
        }
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            filterSensitiveNodeRecursive(rootNode);
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            log.error("过滤敏感信息失败", e);
            return json;
        }
    }

    /**
     * 递归过滤敏感节点（职责单一，逻辑清晰，优化数组处理性能）
     */
    private void filterSensitiveNodeRecursive(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fieldIterator = objectNode.fields();
            while (fieldIterator.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();
                String fieldName = fieldEntry.getKey();
                JsonNode fieldValue = fieldEntry.getValue();

                if (SENSITIVE_PATTERN.matcher(fieldName).matches()) {
                    objectNode.put(fieldName, "***");
                } else {
                    filterSensitiveNodeRecursive(fieldValue);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode elementNode = arrayNode.get(i);
                if (elementNode.isTextual() && containsSensitiveInfo(elementNode.asText())) {
                    arrayNode.set(i, objectMapper.getNodeFactory().textNode("***"));
                } else {
                    filterSensitiveNodeRecursive(elementNode);
                }
            }
        }
    }

    /**
     * 判断字符串是否包含敏感信息（空值安全）
     */
    private boolean containsSensitiveInfo(String value) {
        return StringUtils.hasText(value) && SENSITIVE_PATTERN.matcher(value).find();
    }

    /**
     * 异常信息脱敏处理（空值安全）
     */
    private String sanitizeErrorMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知错误";
        }
        return SENSITIVE_PATTERN.matcher(message).replaceAll("***");
    }

    /**
     * 获取操作对象ID（反射通用化，消除硬编码，空值安全）
     */
    private String getOperObjId(Object[] args, String uri) {
        if (StringUtils.hasText(uri) && uri.contains("/delete/")) {
            String[] uriParts = uri.split("/");
            for (int i = 0; i < uriParts.length - 1; i++) {
                if ("delete".equals(uriParts[i])) {
                    String id = uriParts[i + 1];
                    if (isValidIdFormat(id)) {
                        return id;
                    }
                }
            }
        }

        if (args == null) {
            return EMPTY_STR;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            Class<?> entityClass = arg.getClass();
            if (entityConfigMap.containsKey(entityClass)) {
                EntityConfig config = entityConfigMap.get(entityClass);
                try {
                    Object idValue = getFieldValueByReflection(arg, config.getIdField());
                    if (idValue != null) {
                        String idStr = idValue.toString();
                        if (isValidIdFormat(idStr)) {
                            return idStr;
                        }
                    }
                } catch (Exception e) {
                    log.error("反射提取实体{}ID失败", entityClass.getSimpleName(), e);
                }
            }
        }
        return EMPTY_STR;
    }

    /**
     * 获取操作对象名称（反射通用化，消除硬编码，空值安全）
     */
    public String getOperObjName(Object[] args) {
        if (args == null) {
            return EMPTY_STR;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            Class<?> entityClass = arg.getClass();
            if (entityConfigMap.containsKey(entityClass)) {
                EntityConfig config = entityConfigMap.get(entityClass);
                List<String> bizFields = config.getBizFields();
                Optional<String> nameField = bizFields.stream()
                        .filter(field -> field.endsWith("Name"))
                        .findFirst();
                if (nameField.isPresent()) {
                    try {
                        Object nameValue = getFieldValueByReflection(arg, nameField.get());
                        return nameValue != null ? nameValue.toString() : EMPTY_STR;
                    } catch (Exception e) {
                        log.error("反射提取实体{}名称失败", entityClass.getSimpleName(), e);
                    }
                }
            }
            if (arg instanceof TrainQuality) {
                return "答题质量记录";
            }
        }
        return EMPTY_STR;
    }

    /**
     * 验证ID格式是否合法（空值安全）
     */
    private boolean isValidIdFormat(String id) {
        return StringUtils.hasText(id) && ID_FORMAT_PATTERN.matcher(id).matches();
    }

    /**
     * 生成唯一日志ID（优化算法，避免重复，线程安全）
     */
    private String generateUniqueLogId() {
        long timestamp = System.currentTimeMillis() % 1000000000L;
        int randomNum = ThreadLocalRandom.current().nextInt(1000);
        return String.format(LOG_ID_FORMAT, timestamp, randomNum);
    }

    /**
     * 构建SysOperLog实体（提取公共构建逻辑，消除冗余，空值安全）
     */
    private SysOperLog buildSysOperLog(JwtUserDetails userDetails, String operName, String operIp,
                                       String operMenu, String operType, String targetId,
                                       String targetName, String operContent, String operResult) {
        SysOperLog operLog = new SysOperLog();
        operLog.setLogId(generateUniqueLogId());
        operLog.setOperOrgId(Optional.ofNullable(userDetails).map(JwtUserDetails::getOrgId).orElse(EMPTY_STR));
        operLog.setOperAccount(Optional.ofNullable(userDetails).map(JwtUserDetails::getAccount).orElse(EMPTY_STR));
        operLog.setOperName(Optional.ofNullable(operName).orElse(EMPTY_STR));
        operLog.setOperMenu(Optional.ofNullable(operMenu).orElse(EMPTY_STR));
        operLog.setOperType(Optional.ofNullable(operType).orElse(EMPTY_STR));
        operLog.setTargetId(Optional.ofNullable(targetId).orElse(EMPTY_STR));
        operLog.setTargetName(Optional.ofNullable(targetName).orElse(EMPTY_STR));
        // 适配默认内容
        operLog.setOperContent(Optional.ofNullable(operContent).orElse(
                OperateType.EDIT.getDesc().equals(operType) ? DEFAULT_EDIT_CONTENT : DEFAULT_SIMPLE_CONTENT
        ));
        operLog.setOperIp(Optional.ofNullable(operIp).orElse("0.0.0.0"));
        operLog.setOperTime(LocalDateTime.now());
        operLog.setOperResult(Optional.ofNullable(operResult).orElse(EMPTY_STR));
        return operLog;
    }

    /**
     * 反射提取实体的业务字段值（通用工具方法，支持父类字段，空值安全）
     */
    private void extractBizFieldsByReflection(Object entity, List<String> bizFields, Map<String, Object> targetMap)
            throws Exception {
        if (entity == null || bizFields == null || targetMap == null || bizFields.isEmpty()) {
            return;
        }
        Class<?> entityClass = entity.getClass();
        for (String fieldName : bizFields) {
            Field field = getDeclaredFieldRecursive(entityClass, fieldName);
            if (field == null) {
                log.warn("实体{}未找到字段{}，跳过提取", entityClass.getSimpleName(), fieldName);
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            Object fieldValue = field.get(entity);
            targetMap.put(fieldName, fieldValue);
        }
    }

    /**
     * 递归获取实体字段（支持父类字段，解决继承场景下字段获取失败问题）
     */
    private Field getDeclaredFieldRecursive(Class<?> clazz, String fieldName) {
        if (clazz == null || StringUtils.isEmpty(fieldName)) {
            return null;
        }
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return getDeclaredFieldRecursive(superClass, fieldName);
            }
            return null;
        }
    }

    /**
     * 反射获取实体的主键字段值（通用工具方法，支持父类字段，空值安全）
     */
    private Object getFieldValueByReflection(Object entity, String idFieldName) throws Exception {
        if (entity == null || StringUtils.isEmpty(idFieldName)) {
            return null;
        }
        Class<?> entityClass = entity.getClass();
        Field idField = getDeclaredFieldRecursive(entityClass, idFieldName);
        if (idField == null) {
            log.warn("实体{}未找到主键字段{}", entityClass.getSimpleName(), idFieldName);
            return null;
        }
        idField.setAccessible(true);
        return idField.get(entity);
    }

    /**
     * 反射调用Mapper的selectById方法查询旧数据（通用工具方法，适配MyBatis动态代理，空值安全）
     */
    private Object queryOldEntityByReflection(Object mapper, Object idValue) throws Exception {
        if (mapper == null || idValue == null || !StringUtils.hasText(idValue.toString())) {
            return null;
        }

        Class<?> mapperClass = mapper.getClass();
        Method selectByIdMethod = null;
        for (Class<?> interfaceClz : mapperClass.getInterfaces()) {
            try {
                selectByIdMethod = interfaceClz.getMethod(SELECT_BY_ID_METHOD, Object.class);
                break;
            } catch (NoSuchMethodException e) {
                continue;
            }
        }

        if (selectByIdMethod == null) {
            log.error("Mapper{}未找到{}方法", mapperClass.getSimpleName(), SELECT_BY_ID_METHOD);
            return null;
        }

        return selectByIdMethod.invoke(mapper, idValue);
    }
}