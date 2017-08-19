package com.ezlipp.javaagent;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * A Transformer add some metrics for every method
 */
public class MetricsTransfomer implements ClassFileTransformer {
    public byte[] transform(ClassLoader loader, String className, Class<?>
            classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] byteCode) throws IllegalClassFormatException {
        byte[] transformed = byteCode;
        //skip java class transform
        if(className.startsWith("java") || className.startsWith("sun")){
            return transformed;
        }
        System.out.println("transform class:" + className);
        //use javassist to generate class dynamic
        ClassPool pool = ClassPool.getDefault();
        CtClass cl = null;
        try {
            cl = pool.makeClass(new java.io.ByteArrayInputStream(
                    byteCode));
            if (!cl.isInterface()) {
                CtBehavior[] methods = cl.getDeclaredBehaviors();
                for (CtBehavior method : methods) {
                    if (!method.isEmpty()) {
                        modifyMethod(method);
                    }
                }
                transformed = cl.toBytecode();
            }
        } catch (Exception e) {
            System.err.println("Could not instrument  " + className
                    + ",  exception : " + e.getMessage());
        } finally {
            if (cl != null) {
                cl.detach();
            }
        }

        return transformed;
    }

    private void modifyMethod(CtBehavior method) throws NotFoundException,
            CannotCompileException {
        // method.insertBefore("long stime = System.nanoTime();");
        // method.insertAfter("System.out.println(\"leave "+method.getName()+" and time:\"+(System.nanoTime()-stime));");
        method.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                m.replace("{ long stime = System.nanoTime(); $_ = $proceed($$); System.out.println(\""
                        + m.getClassName()+"."+m.getMethodName()
                        + ":\"+(System.nanoTime()-stime));}");
            }
        });
    }
}
