package cn.xu.spring.context;

import cn.xu.spring.annotation.*;
import cn.xu.spring.exception.BeanDefinitionException;
import cn.xu.spring.exception.BeanNotOfRequiredTypeException;
import cn.xu.spring.exception.NoUniqueBeanDefinitionException;
import cn.xu.spring.io.PropertyResolver;
import cn.xu.spring.io.ResourceResolver;
import cn.xu.spring.utils.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * created by Xu on 2024/5/6 20:50.
 */

public class AnnotationConfigApplicationContext {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    protected final PropertyResolver propertyResolver;

    protected final Map<String, BeanDefinition> beans;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;

        // 获取所有Bean的Class类型
        final Set<String> beanClassNames = scanForClassNames(configClass);

        // 创建BeanDefinition
        this.beans = createBeanDefinitions(beanClassNames);
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

}
