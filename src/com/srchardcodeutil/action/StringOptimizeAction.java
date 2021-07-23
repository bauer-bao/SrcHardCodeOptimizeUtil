package com.srchardcodeutil.action;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.srchardcodeutil.bean.Entity;
import com.srchardcodeutil.util.Util;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * layout文件夹转成strings
 * Created by bauer on 2019/11/25.
 */
public class StringOptimizeAction extends AnAction {
    /**
     * 记录已经遍历的entity列表
     */
    private List<Entity> entityList;
    private int index = 0;

    @Override
    public void actionPerformed(AnActionEvent e) {
        //action具体执行逻辑
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (file == null) {
            //文件不存在
            Util.showError("File is not exist");
            return;
        }
        VirtualFile parentFile = file.getParent();
        if (file.isDirectory()) {
            //是文件夹
            if (!file.getName().startsWith("layout") && !file.getName().equals("res")) {
                //不支持
                Util.showError("Operation is not support");
                return;
            }
        } else {
            //是文件
            if (!parentFile.getName().startsWith("layout")) {
                //不支持
                Util.showError("Operation is not support");
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        entityList = Lists.newArrayList();
        if (file.isDirectory()) {
            //获取全部子文件
            VirtualFile[] children = file.getChildren();
            //遍历子文件，找到需要替换的资源并替换，并且记录到entityList中
            for (VirtualFile child : children) {
                if (child.isDirectory()) {
                    if (child.getName().startsWith("layout")) {
                        //孙类，并且是layout
                        VirtualFile[] children2 = child.getChildren();
                        for (VirtualFile child2 : children2) {
                            //说明对res文件夹进行的操作
                            scanChildFile(child2, sb);
                        }
                    }
                } else {
                    //说明对layout的单文件进行的操作
                    scanChildFile(child, sb);
                }
            }
            //将对应的资源写入文件
            if (file.getName().equals("res")) {
                //当前目录就是res，则不需要父文件
                Util.saveToFile(file, sb, true);
            } else {
                //是res的子目录，需要父文件
                Util.saveToFile(parentFile, sb, true);
            }
        } else {
            //单个文件
            scanChildFile(file, sb);
            //将对应的资源写入文件
            Util.saveToFile(parentFile.getParent(), sb, true);
        }
        //刷新整个工程的文件
        e.getActionManager().getAction(IdeActions.ACTION_SYNCHRONIZE).actionPerformed(e);
        Util.showTip("Optimize finish");
    }

    /**
     * 扫描文件
     *
     * @param file
     * @param sb
     */
    private void scanChildFile(VirtualFile file, StringBuilder sb) {
        //重置索引
        index = 0;
        //获取后缀名
        String extension = file.getExtension();
        if (extension == null || !extension.equalsIgnoreCase("xml")) {
            //后缀名不是xml
            return;
        }
        if (!file.getParent().getName().startsWith("layout")) {
            //如果不是layout文件，则不处理
            Util.showError("Operation is not support");
            return;
        }
        StringBuilder oldContent = new StringBuilder();
        try {
            //将待优化文件转成string
            oldContent.append(new String(file.contentsToByteArray(), StandardCharsets.UTF_8));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        InputStream is = null;
        try {
            is = file.getInputStream();
            //获取一个文件中的全部新增的资源列表
            List<Entity> result = extraEntity(is, file.getNameWithoutExtension().toLowerCase(), oldContent);
            //保存到全部文件的资源列表中
            entityList.addAll(result);
            for (Entity string : result) {
                sb.append("\n    <string name=\"")
                        .append(string.getId())
                        .append("\">")
                        .append(string.getValue())
                        .append("</string>");
            }
            //更新文件
            Util.saveContentToFile(file.getPath(), oldContent.toString());
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取资源，并且生成entity对象
     *
     * @param is
     * @param fileName
     * @param oldContent
     * @return
     */
    private List<Entity> extraEntity(InputStream is, String fileName, StringBuilder oldContent) {
        List<Entity> strings = Lists.newArrayList();
        try {
            return generateStrings(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is), strings, fileName, oldContent);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 遍历整个文件
     *
     * @param node
     * @param strings
     * @param fileName
     * @param oldContent
     * @return
     */
    private List<Entity> generateStrings(Node node, List<Entity> strings, String fileName, StringBuilder oldContent) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            //处理text属性
            String targetItem = "android:text";
            oldContent = scanNode(node, fileName, oldContent, strings, targetItem);
            //处理hint属性
            targetItem = "android:hint";
            oldContent = scanNode(node, fileName, oldContent, strings, targetItem);
        } else if (node.getNodeType() == Node.COMMENT_NODE) {
            /*
             * 为什么要处理注释中的硬编码？可以换个问题，不处理会有什么影响？
             * 注释中会存在硬编码的原因，1.纯注释，2.可能还有用的代码，暂时先被注释了，如果没用的代码肯定是选择删除，而不是注释。
             * 所以如果插件不处理注释，然后开发者再次使用了注释中的代码，那么项目中就依然存在硬编码。
             * 如果处理了，就算重新使用注释代码，也不会有任何问题，如果真多余了，最多再去资源文件中删除对应的数据即可
             *
             * 注释默认一行就是一个comment node
             * 方案1：补成node的形式，简单，很多情况会爆红，但是不影响整个流程。
             * 方案2：遍历字符串，算法复杂---【采用方案】
             */
            String commentValue = node.getNodeValue();
            //处理text属性
            String targetItem = "android:text";
            if (commentValue.contains(targetItem)) {
                oldContent = scanComment(commentValue, fileName, oldContent, strings, targetItem);
            }
            //处理hint属性
            targetItem = "android:hint";
            if (commentValue.contains(targetItem)) {
                oldContent = scanComment(commentValue, fileName, oldContent, strings, targetItem);
            }
        }
        //继续遍历子节点
        NodeList children = node.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            generateStrings(children.item(j), strings, fileName, oldContent);
        }
        return strings;
    }

    /**
     * 扫描node节点
     *
     * @param node
     * @param fileName
     * @param oldContent
     * @param strings
     * @param targetItem
     * @return
     */
    private StringBuilder scanNode(Node node, String fileName, StringBuilder oldContent, List<Entity> strings, String targetItem) {
        Node stringNode = node.getAttributes().getNamedItem(targetItem);
        if (stringNode != null) {
            String value = stringNode.getNodeValue();
            oldContent = replaceContent(value, fileName, oldContent, strings, targetItem);
        }
        return oldContent;
    }

    /**
     * 扫描node节点
     *
     * @param comment
     * @param fileName
     * @param oldContent
     * @param strings
     * @param targetItem
     * @return
     */
    private StringBuilder scanComment(String comment, String fileName, StringBuilder oldContent, List<Entity> strings, String targetItem) {
        int targetIndex = 0;
        while (targetIndex < comment.length() && targetIndex >= 0) {
            targetIndex = comment.indexOf(targetItem, targetIndex);
            if (targetIndex != -1) {
                //存在目标属性，开始找对应的value
                StringBuilder value = new StringBuilder();
                //获取value开始的index
                int startIndex = Util.getValueIndex(comment, targetIndex + targetItem.length());
                if (startIndex != -1) {
                    for (int i = startIndex; i < comment.length(); i++) {
                        if (comment.charAt(i) == '"') {
                            break;
                        } else {
                            value.append(comment.charAt(i));
                        }
                    }
                    //成功获取到value
                    String valueStr = value.toString();
                    //替换value
                    oldContent = replaceContent(valueStr, fileName, oldContent, strings, targetItem);
                    //继续查找下一个值，如果 == 0 说明是空格，但是还是要继续查找下一个值
                    if (valueStr.length() > 0) {
                        targetIndex += valueStr.length();
                    } else {
                        targetIndex++;
                    }
                } else {
                    targetIndex++;
                }
            }
        }
        return oldContent;
    }

    /**
     * 替换内容
     *
     * @param value
     * @param fileName
     * @param oldContent
     * @param strings
     * @param targetItem
     * @return
     */
    private StringBuilder replaceContent(String value, String fileName, StringBuilder oldContent, List<Entity> strings, String targetItem) {
        if (value.length() > 0 &&
                !value.contains("@string") &&
                !(value.startsWith("@{") && value.endsWith("}")) &&
                !(value.startsWith("@={") && value.endsWith("}"))) {
            //为空，或者已经有@string 或者是 databinding的样式，就不需要处理，反之需要处理
            List<Entity> queryList = Lists.newArrayList();
            queryList.addAll(entityList);
            queryList.addAll(strings);
            String targetId = null;
            //检查当前的value是否已经存在，entityList是已经遍历过文件的列表，strings是当前遍历的文件的列表
            for (Entity entity : queryList) {
                if (entity.getValue().equals(value)) {
                    //已经存在的value
                    targetId = entity.getId();
                    break;
                }
            }
            if (targetId == null || targetId.length() == 0) {
                //不存在
                targetId = fileName + "_text_" + (index++);
                strings.add(new Entity(targetId, value));
            }
            int index = 0;
            while (index < oldContent.length() && index >= 0) {
                index = Util.getRightIndex(oldContent, value, targetItem, 0);
                if (index != -1) {
                    //说明找到对应的值 +2 是因为替换的是 "value"，而不是value
                    oldContent = oldContent.replace(index, index + value.length() + 2, "\"@string/" + targetId + "\"");
                    //继续查找下一个值
                    index += value.length();
                }
            }
        }
        return oldContent;
    }
}