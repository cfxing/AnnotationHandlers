package com.xnj.handler;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementScanner8;
import javax.tools.Diagnostic;
import java.util.EnumSet;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * 程序名称规范的编译器插件：
 * 如果程序命名不和规范，将会输出一个编译器的WARNING 信息
 *
 * @author chen xuanyi
 * @create 2020-11-09 18:11
 */
public class NameChecker {
    private final Messager messager;

    NameCheckScanner nameCheckScanner = new NameCheckScanner();


    public NameChecker(ProcessingEnvironment processingEnv) {
        this.messager = processingEnv.getMessager();
    }

    /**
     * 对于java程序命名进行检查,应该满足以下规则
     *
     * 类或接口：驼峰式命名，首字母大写
     * 方法：驼峰式命名，首字母小写
     * 常量：全部大写
     * 变量：驼峰式命名，首字母小写
     *
     */
    public void checkNames(Element element) {
        nameCheckScanner.scan(element);
    }

    //名称检查器实现类，继承ElementScanner8
    //将会以Visitor模式访问抽象语法树的元素
    private class NameCheckScanner extends ElementScanner8<Void, Void> {

        //用于检查java类
        @Override
        public Void visitType(TypeElement e, Void aVoid) {
            scan(e.getTypeParameters(), aVoid);
            checkCamelCase(e, true);
            super.visitType(e, aVoid);
            return null;
        }

        //检查方法命名

        @Override
        public Void visitExecutable(ExecutableElement e, Void aVoid) {
            if (e.getKind() == ElementKind.METHOD){
                Name name = e.getSimpleName();
                if (name.contentEquals(e.getEnclosingElement().getSimpleName())){
                    messager.printMessage(Diagnostic.Kind.WARNING, "一个普通方法" + name +"不应该与类名重复，避免与构造函数产生混淆", e);
                }
                checkCamelCase(e, false);
            }
            super.visitExecutable(e, aVoid);
            return null;
        }

        //检查变量命名是否合法
        @Override
        public Void visitVariable(VariableElement e, Void aVoid) {
            //如果 Variable 是枚举或常量，则按大写命名检查，否则按驼峰式检查
            if (e.getKind() == ElementKind.ENUM_CONSTANT || e.getConstantValue() != null || heuristicallyConstant(e)){
                checkAllCaps(e);
            }else{
                checkCamelCase(e,false);
            }
            return super.visitVariable(e, aVoid);
        }

        //判断一个变量是否是常量
        private boolean heuristicallyConstant(VariableElement e){
            if (e.getEnclosingElement().getKind() == ElementKind.INTERFACE) {
                return true;
            }else if (e.getKind() == ElementKind.FIELD && e.getModifiers().containsAll(EnumSet.of(PUBLIC, STATIC, FINAL))){
                return true;
            }else {
                return false;
            }
        }

        //检查传入的Element 是否符合驼峰式命名，如果不符合，则输出警告信息
        private void checkCamelCase(Element e, boolean initialCaps){
            String name = e.getSimpleName().toString();
            boolean previousUpper = false;
            boolean conventional = true;
            int firstCodePoint = name.codePointAt(0);

            /**
             * 如果一开始的首字母为大写，但发现initialCaps 为false，表示它不应该为大写，则输出警告信息
             * 如果一开始的首字母为小写，但发现initialCaps 为true，表示它应该为大写，则输出警告信息
             */
            if (Character.isUpperCase(firstCodePoint)){
                previousUpper = true;
                if (!initialCaps) {
                    messager.printMessage(Diagnostic.Kind.WARNING, "名称" + name + "应该以小写字母开头", e);
                    return;
                }
            }else if (Character.isLowerCase(firstCodePoint)){
                if (initialCaps) {
                    messager.printMessage(Diagnostic.Kind.WARNING, "名称" + name + "应该以大写字母开头", e);
                    return;
                }
            }else{
                conventional = false;
            }

            if (conventional) {
                int cp = firstCodePoint;
                for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
                    cp = name.codePointAt(i);
                    if (Character.isUpperCase(cp)){
                        if (previousUpper){
                            conventional = false;
                            break;
                        }
                        previousUpper = true;
                    }else{
                        previousUpper = false;
                    }
                }
            }

            if (!conventional) {
                messager.printMessage(Diagnostic.Kind.WARNING, "名称" + name + "应该符合驼峰式命名", e);
            }
        }

        //大写命名检查，要求第一个字母为大写字母，其余部分为下划线或大写字母
        private void checkAllCaps(Element e){
            String name = e.getSimpleName().toString();

            boolean conventional = true;
            //指向第一个字符
            int firstCodePoint = name.codePointAt(0);

            if (!Character.isUpperCase(firstCodePoint)){
                conventional = false;
            }else{
                //防止出现连续的下划线
                boolean previousUnderscore = false;
                int cp = firstCodePoint;
                for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)){
                    cp = name.codePointAt(i);
                    if (cp == (int) '_'){
                        if (previousUnderscore) {
                            conventional = false;
                            break;
                        }
                        previousUnderscore = true;
                    }else{
                        previousUnderscore = false;
                        if (!Character.isUpperCase(cp) && !Character.isDigit(cp)){
                            conventional = false;
                            break;
                        }
                    }
                }
            }

            if (!conventional){
                messager.printMessage(Diagnostic.Kind.WARNING, "常量" + name + "应该全部以大写字母或下划线命名，并以大写字母开头", e);
            }
        }
    }
}
