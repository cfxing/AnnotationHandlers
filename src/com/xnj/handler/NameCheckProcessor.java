package com.xnj.handler;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * 插入式注解处理器，实现java程序命名规范检查
 * 类或接口：驼峰式，首字母大写
 * 方法：驼峰式，首字母小写
 * 字段：
 *  类或实例变量：驼峰式，首字母小写
 *  常量：全部由大写或下划线组成，首字母为大写字母
 *
 * @author chen xuanyi
 * @create 2020-11-09 18:07
 */
//用 "*" 表示支持所有的Annotations
@SupportedAnnotationTypes("*")
//只支持 JDK8 的代码
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class NameCheckProcessor extends AbstractProcessor {
    private NameChecker nameChecker;

    //初始化名称检查插件

    /**
     *
     * @param processingEnv AbstractProcessor 中的一个protected变量，在注解处理器初始化的时候创建，
     *                      代表了注解处理器框架提供的一个上下文环境，要创建新代码，向编译器输出信息，
     *                      获取其他工具类等都需要用到这个实例变量；
     */
    @Override
    public void init(ProcessingEnvironment processingEnv){
        super.init(processingEnv);
        nameChecker = new NameChecker(processingEnv);
    }

    //对输入的语法书的各个节点进行名称检查

    /**
     *
     * @param annotations  次注解处理器所要处理的注解集合
     * @param roundEnv  访问到当前Round 中的语法树的节点，每个语法树节点表示为一个Element
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        if (!roundEnv.processingOver()){
            for (Element element: roundEnv.getRootElements()){
                nameChecker.checkNames(element);
            }
        }
        //因为注解处理器只对程序命名进行检查，不需要改变语法树的内容，所以process的返回一直为false;
        return false;
    }
}
