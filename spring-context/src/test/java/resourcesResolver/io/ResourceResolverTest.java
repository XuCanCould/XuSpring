package resourcesResolver.io;

import cn.xu.spring.io.ResourceResolver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.annotation.sub.AnnoScan;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceResolverTest {

    @Test
    public void scanClass() {
        var pkg = "resourcesResolver.scan";
        var rr = new ResourceResolver(pkg);
        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        Collections.sort(classes);
        System.out.println(classes);
        String[] listClasses = new String[] {
                // list of some scan classes:
                "resourcesResolver.scan.convert.ValueConverterBean", //
                "resourcesResolver.scan.destroy.AnnotationDestroyBean", //
                "resourcesResolver.scan.init.SpecifyInitConfiguration", //
                "resourcesResolver.scan.proxy.OriginBean", //
                "resourcesResolver.scan.proxy.FirstProxyBeanPostProcessor", //
                "resourcesResolver.scan.proxy.SecondProxyBeanPostProcessor", //
                "resourcesResolver.scan.nested.OuterBean", //
                "resourcesResolver.scan.nested.OuterBean$NestedBean", //
                "resourcesResolver.scan.sub1.Sub1Bean", //
                "resourcesResolver.scan.sub1.sub2.Sub2Bean", //
                "resourcesResolver.scan.sub1.sub2.sub3.Sub3Bean", //
        };
        for (String clazz : listClasses) {
            assertTrue(classes.contains(clazz));
        }
    }

    @Test
    public void scanJar() {
        var pkg = PostConstruct.class.getPackageName();
        var rr = new ResourceResolver(pkg);
        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        // classes in jar:
        assertTrue(classes.contains(PostConstruct.class.getName()));
        assertTrue(classes.contains(PreDestroy.class.getName()));
        assertTrue(classes.contains(PermitAll.class.getName()));
        assertTrue(classes.contains(DataSourceDefinition.class.getName()));
        // jakarta.annotation.sub.AnnoScan is defined in classes:
        assertTrue(classes.contains(AnnoScan.class.getName()));
    }

    @Test
    public void scanTxt() {
        var pkg = "resourcesResolver.scan";
        var rr = new ResourceResolver(pkg);
        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".txt")) {
                return name.replace("\\", "/");
            }
            return null;
        });
        Collections.sort(classes);
        assertArrayEquals(new String[] {
                // txt files:
                "resourcesResolver/scan/sub1/sub1.txt", //
                "resourcesResolver/scan/sub1/sub2/sub2.txt", //
                "resourcesResolver/scan/sub1/sub2/sub3/sub3.txt", //
        }, classes.toArray(String[]::new));
    }
}
