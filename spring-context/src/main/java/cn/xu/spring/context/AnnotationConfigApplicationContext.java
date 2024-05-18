package cn.xu.spring.context;

import cn.xu.spring.annotation.*;
import cn.xu.spring.exception.*;
import cn.xu.spring.io.PropertyResolver;
import cn.xu.spring.io.ResourceResolver;
import cn.xu.spring.utils.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * created by Xu on 2024/5/6 20:50.
 */

public class AnnotationConfigApplicationContext {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    protected final PropertyResolver propertyResolver;

    protected final Map<String, BeanDefinition> beans;

    private Set<String> creatingBeanNames;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;

        // 获取所有Bean的Class类型
        final Set<String> beanClassNames = scanForClassNames(configClass);

        // 创建BeanDefinition
        this.beans = createBeanDefinitions(beanClassNames);

        // 创建BeanName检测循环依赖:
        this.creatingBeanNames = new HashSet<>();

        // 创建 @configuration 类型的bean
        this.beans.values().stream()
            // 过滤出 @configuration 类型的bean
            .filter(this::isConfigurationDefinition).sorted().map(bd -> {
                createBeanAsEarlySingleton(bd);
                return bd.getName();
            }).collect(Collectors.toList());

        // 创建其他bean
        createNormalBeans();

        if (logger.isDebugEnabled()) {
            beans.values().forEach(bd -> logger.debug("bean initialized: {}", bd));
        }
    }

    void createNormalBeans() {
        List<BeanDefinition> unInstance = this.beans.values().stream().
                filter(bd -> bd.getInstance() == null).sorted().collect(Collectors.toList());
        unInstance.forEach(bd -> {
            // 如果Bean未被创建(可能在其他Bean的构造方法注入前被创建):
            if(bd.getInstance() == null) {
                createBeanAsEarlySingleton(bd);
            }
        });
    }

    /**
     * 根据扫描的 beanClassNames 创建 BeanDefinition
     * @param beanClassNames
     * @return
     */
    Map<String, BeanDefinition> createBeanDefinitions(Set<String> beanClassNames) {
        HashMap<String, BeanDefinition> defs = new HashMap<>();
        for (String beanClassName : beanClassNames) {
            Class clazz = null;
            try {
                clazz = Class.forName(beanClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isRecord() || clazz.isInterface()) {
                continue;
            }
            // 判断是否为组件
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                logger.atDebug().log("found component: {}", clazz.getName());
                // 得到类的修饰符
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
                }

                String beanName = ClassUtils.getBeanName(clazz);
                BeanDefinition beanDefinition = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz),
                        getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        null, null,
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                addBeanDefinitions(defs, beanDefinition);
                logger.atDebug().log("define bean: {}", beanDefinition);

                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    scanFactoryMethods(beanName, clazz, defs);
                }
            }
        }
        return defs;
    }


    /**
     * 得到合适（注意处理私有的，并且要求无参）的构造方法
     * @param clazz
     * @return
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        // 没有获得公开的构造方法
        if (constructors.length == 0) {
            constructors = clazz.getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if (constructors.length != 1) {
            throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
        }
        return constructors[0];
    }

    /**
     * 扫描工厂方法
     */
    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, HashMap<String, BeanDefinition> defs) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            Bean bean = method.getAnnotation(Bean.class);
            // 是否被 bean 注释
            if (bean != null) {
                // 检查修饰符
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
                }
                if (Modifier.isFinal(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
                }
                // 工厂方法的返回类型不应该是基本类型（primitive type）也不应该返回空类型（void）
                Class<?> classType = method.getReturnType();
                if (classType.isPrimitive()) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
                }
                if (classType == void.class || classType == Void.class) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
                }
                // 创建 beanDefinition
                var def = new BeanDefinition(ClassUtils.getBeanName(method), classType, factoryBeanName, method,
                        getOrder(method), method.isAnnotationPresent(Primary.class),
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        null, null);
                addBeanDefinitions(defs, def);
                logger.atDebug().log("define bean: {}", def);
            }
        }
    }


    void addBeanDefinitions(HashMap<String, BeanDefinition> defs, BeanDefinition news) {
        if (defs.put(news.getName(), news) != null) {
            throw new BeanDefinitionException("Duplicate bean name: "  + news.getName());
        }
    }

    /**
     * 得到（@Order被注释在类上的） order 的值
     */
    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }


    /**
     * 得到（@Order被注释在方法上的） order 的值
     */
    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }



    /**
     * 扫描组件并返回类名
     */
    Set<String> scanForClassNames(Class<?> configClass) {
        ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        // 获取配置包名，没有标注则默认当前包名
        final String[] scanPackages = scan == null || scan.value().length == 0 ?
                new String[] { configClass.getPackage().getName() } : scan.value();
        logger.atInfo().log("component scan in packages: {}", Arrays.toString(scanPackages));

        Set<String> classNameSet = new HashSet<>();
        for (String pkg : scanPackages) {
            logger.atDebug().log("scan package: {}", pkg);
            ResourceResolver rr = new ResourceResolver(pkg);
            List<String> classList = rr.scan(resource -> {
                String name = resource.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });
            if (logger.isDebugEnabled()) {
                classList.forEach((className) -> {
                    logger.debug("class found by component scan: {}", className);
                });
            }
            classNameSet.addAll(classList);
        }

        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importClass : importConfig.value()) {
                String importClassName = importClass.getName();
                if (classNameSet.contains(importClassName)) {
                    logger.warn("ignore import: " + importClassName + " for it is already been scanned.");
                } else {
                    logger.debug("class found by import: {}", importClassName);
                    classNameSet.add(importClassName);
                }
            }
        }
        return classNameSet;
    }

    boolean isConfigurationDefinition(Class<?> clazz) {
        return ClassUtils.findAnnotation(clazz, Configuration.class) != null;
    }

    /**
     * 根据 name 查找 BeanDefinition
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String beanName) {
        return this.beans.get(beanName);
    }

    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition definition = findBeanDefinition(name);
        if (definition == null) {
            return null;
        }
        if (requiredType.isAssignableFrom(definition.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.", requiredType.getName(),
                    name, definition.getBeanClass().getName()));
        }
        return definition;
    }

    /**
     * 根据 Type 查找若干个 BeanDefinition
     */
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                .sorted().collect(Collectors.toList());
    }

    /**
     * 根据 Type 查找一个 BeanDefinition，当存在多个 BeanDefinition：
     * 1、返回被 @Primary 标注的（要求唯一）
     * 2、如果存在多个该注释抛出异常
     * 3、未进行注释同样抛出异常
     */
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> beanDefinitions = findBeanDefinitions(type);
        if (beanDefinitions.isEmpty()) {
            return null;
        }
        if (beanDefinitions.size() == 1) {
            return beanDefinitions.get(0);
        }
        List<BeanDefinition> primaryDefs = beanDefinitions.stream().filter(def -> def.isPrimary()).collect(Collectors.toList());
        if (primaryDefs.size() == 1) {
            return primaryDefs.get(0);
        }
        if (primaryDefs.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }


    boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    /**
     * bean 的创建，需要处理循环依赖的问题
     */
    public Object createBeanAsEarlySingleton(BeanDefinition definition) {
        logger.atDebug().log("Try create bean '{}' as early singleton: {}", definition.getName(), definition.getBeanClass().getName());
        if (!creatingBeanNames.add(definition.getName())) {
            throw new UnsatisfiedDependencyException(
                    String.format("Circular dependency detected when create bean '%s'", definition.getName()));
        }

        // 得到创建bean的方法，构造方法/工厂方法
        Executable createFn = null;
        if (definition.getFactoryName() == null) {
            createFn = definition.getConstructor();
        } else {
            createFn = definition.getFactoryMethod();
        }

        // 处理创建bean的参数
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] annotations = createFn.getParameterAnnotations();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i ++) {
            final Parameter parameter = parameters[i];
            final Annotation[] paramAnnos = annotations[i];
            // 这里只处理被 @Autowired、@Value 注解的参数
            final Value value = ClassUtils.getAnnotation(paramAnnos, Value.class);
            final Autowired autowired = ClassUtils.getAnnotation(paramAnnos, Autowired.class);

            // @Configuration类型的Bean是工厂，不允许使用@Autowired创建
            final boolean isConfiguration = isConfigurationDefinition(definition);
            if (isConfiguration && autowired != null) {
                throw new BeanCreationException(String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.",
                        definition.getName(), definition.getBeanClass().getName()));
            }

            // 对注解 @Value、@Autowired 的要求
            if (value == null && autowired == null) {
                String.format("Must specify @Autowired or @Value when create bean '%s': %s.", definition.getName(), definition.getBeanClass().getName());
            }
            if (value != null && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.",
                                definition.getName(), definition.getBeanClass().getName()));
            }

            final Class<?> type = parameter.getType();
            if (value != null) { // @Value
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            } else { // @Autowired
                String name = autowired.name();
                boolean required = autowired.value();
                BeanDefinition dependsOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                if (required && dependsOnDef == null) {
                    throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.",
                            type.getName(), definition.getName(), definition.getBeanClass().getName()));
                }
                if (dependsOnDef != null) {
                    Object autowiredBeanInstance = dependsOnDef.getInstance();
                    if (autowiredBeanInstance == null) {
                        // 还没有被初始化，则 createBeanAsEarlySingleton 创建
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependsOnDef);
                    }
                    args[i] = autowiredBeanInstance;
                } else {
                    args[i] = null;
                }
            }
        }

        // 创建bean对象
        Object instance = null;
        // 构造方法
        if (definition.getFactoryName() == null) {
            try {
                instance = definition.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", definition.getName(), definition.getBeanClass().getName()), e);
            }
        } else {
            // 工厂方法
            Object configInstance = getBean(definition.getFactoryName());
            try {
                instance = definition.getFactoryMethod().invoke(configInstance, args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", definition.getName(), definition.getBeanClass().getName()), e);
            }
        }

        definition.setInstance(instance);
        return definition.getInstance();
    }

    /**
     * 注入属性
     */
    void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws ReflectiveOperationException {
        for(Field field : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, field);
        }
        for (Method method : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, method);
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            injectProperties(def, superclass, bean);
        }
    }

    void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws ReflectiveOperationException {
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        if (value == null && autowired == null) {
            return;
        }

        Field field = null;
        Method method = null;
        if (acc instanceof Field f) {
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if (acc instanceof Method m) {
            checkFieldOrMethod(m);
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(
                        String.format("Cannot inject a non-setter method %s for bean '%s': %s",
                                m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }
        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessibleType = field != null ? field.getType() : method.getParameters()[0].getType();

        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        if (value != null) {
            Object property = this.propertyResolver.getRequiredProperty(value.value(), accessibleType);
            if (field != null) {
                logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, property);
                field.set(bean, property);
            } else {
                logger.atDebug().log("Method injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, property);
                method.invoke(bean, property);
            }
        }

        if (autowired != null) {
            String name = autowired.name();
            boolean required = autowired.value();
            Object depends = name.isEmpty() ? findBean(accessibleType) : findBean(name, accessibleType);

            if (required && depends == null) {
                throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.",
                        accessibleType.getName(), def.getName(), def.getBeanClass().getName()));
            }

            if (depends != null) {
                if (field != null) {
                    logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, depends);
                    field.set(bean, depends);
                }
                if (method != null) {
                    logger.atDebug().log("Method injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, depends);
                    method.invoke(bean, depends);
                }
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> type) {
        BeanDefinition definition = findBeanDefinition(name, type);
        if (definition == null) {
            return null;
        }
        return (T) definition.getRequiredInstance();
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition beanDefinition = this.beans.get(name);
        if (beanDefinition == null) {
            throw new BeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) beanDefinition.getInstance();
    }

    /**
     * 检测是否存在指定Name的Bean
     */
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }


    void checkFieldOrMethod(Member member) {
        int modifiers = member.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            throw new BeanDefinitionException("Cannot inject static field: " + modifiers);
        }
        if (Modifier.isFinal(modifiers)) {
            if (member instanceof Field field) {
                throw new BeanDefinitionException("Cannot inject final field: " + field);
            }
            if (member instanceof Method) {
                logger.warn("Inject final method should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException.");
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> List<T> findBeans(Class<T> requiredType) {
        return findBeanDefinitions(requiredType).stream().
                map(bd -> (T) bd.getRequiredInstance()).collect(Collectors.toList());
    }
}
