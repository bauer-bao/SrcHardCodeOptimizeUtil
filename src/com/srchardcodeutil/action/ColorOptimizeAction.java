package com.srchardcodeutil.action;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.srchardcodeutil.bean.Entity;
import com.srchardcodeutil.util.Util;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * layout和drawable文件夹转成colors
 * Created by bauer on 2019/11/25.
 */
public class ColorOptimizeAction extends AnAction {
    /**
     * 记录已经遍历的entity列表
     */
    private List<Entity> entityList;
    private Pattern pattern = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");

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
            if (!file.getName().startsWith("layout") && !file.getName().startsWith("drawable") &&
                    !file.getName().equals("res")) {
                //不支持
                Util.showError("Operation is not support");
                return;
            }
        } else {
            //是文件
            if (!parentFile.getName().startsWith("layout") && !parentFile.getName().startsWith("drawable")) {
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
                    if (child.getName().startsWith("layout") || child.getName().startsWith("drawable")) {
                        //孙类，并且是layout或者drawable
                        VirtualFile[] children2 = child.getChildren();
                        for (VirtualFile child2 : children2) {
                            //说明对res文件夹进行的操作
                            scanChildFile(child2, sb);
                        }
                    }
                } else {
                    //说明对layout或者drawable的单文件进行的操作
                    scanChildFile(child, sb);
                }
            }
            //将对应的资源写入文件
            if (file.getName().equals("res")) {
                //当前目录就是res，则不需要父文件
                Util.saveToFile(file, sb, false);
            } else {
                //是res的子目录，需要父文件
                Util.saveToFile(parentFile, sb, false);
            }
        } else {
            //单个文件
            scanChildFile(file, sb);
            //将对应的资源写入文件
            Util.saveToFile(parentFile.getParent(), sb, false);
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
        //获取后缀名
        String extension = file.getExtension();
        if (extension == null || !extension.equalsIgnoreCase("xml")) {
            //后缀名不是xml
            return;
        }
        if (!file.getParent().getName().startsWith("layout") && !file.getParent().getName().startsWith("drawable")) {
            //如果不是layout或者drawable文件，则不处理
            Util.showError("Operation is not support");
            return;
        }
        StringBuilder oldContent = new StringBuilder();
        try {
            //将待优化文件转成strings
            oldContent.append(new String(file.contentsToByteArray(), StandardCharsets.UTF_8));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        InputStream is = null;
        try {
            is = file.getInputStream();
            //获取一个文件中的全部新增的资源列表
            List<Entity> result = extraEntity(is, oldContent);
            //保存到全部文件的资源列表中
            entityList.addAll(result);
            for (Entity string : result) {
                sb.append("\n    <color name=\"")
                        .append(string.getId())
                        .append("\">")
                        .append(string.getValue())
                        .append("</color>");
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
     * @param oldContent
     * @return
     */
    private List<Entity> extraEntity(InputStream is, StringBuilder oldContent) {
        List<Entity> colors = Lists.newArrayList();
        try {
            return generateColors(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is), colors, oldContent);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 遍历整个文件
     *
     * @param node
     * @param colors
     * @param oldContent
     * @return
     */
    private List<Entity> generateColors(Node node, List<Entity> colors, StringBuilder oldContent) {
        boolean isFiltered = false;
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String name = node.getNodeName();
            if (!name.equals("vector") && !name.equals("group") && !name.equals("path")) {
                //属于svg的xml，不需要处理，反之需要处理
                NamedNodeMap namedNodeMap = node.getAttributes();
                //遍历node下的全部属性
                for (int i = 0; i < namedNodeMap.getLength(); i++) {
                    Node currentNode = namedNodeMap.item(i);
                    oldContent = scanNode(currentNode, oldContent, colors);
                }
            } else {
                isFiltered = true;
            }
        } else if (node.getNodeType() == Node.COMMENT_NODE) {
            /*
             * 处理方式和string不同，1.获取value，2.获取：=之间的值作为属性，3.替换
             */
            String commentValue = node.getNodeValue();
            oldContent = scanComment(commentValue, oldContent, colors);
        }
        //继续遍历子节点
        if (!isFiltered) {
            //如果没有被过滤，则继续遍历子节点
            NodeList children = node.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                generateColors(children.item(j), colors, oldContent);
            }
        }
        return colors;
    }

    /**
     * 扫描node节点
     *
     * @param node
     * @param oldContent
     * @param colors
     * @return
     */
    private StringBuilder scanNode(Node node, StringBuilder oldContent, List<Entity> colors) {
        if (node != null) {
            String value = node.getNodeValue();
            oldContent = replaceContent(value, oldContent, colors, node.getNodeName());
        }
        return oldContent;
    }

    /**
     * 扫描node节点
     *
     * @param comment
     * @param oldContent
     * @param colors
     * @return
     */
    private StringBuilder scanComment(String comment, StringBuilder oldContent, List<Entity> colors) {
        int valueStart = 0;
        while (valueStart < comment.length() && valueStart >= 0) {
            //获取value
            valueStart = comment.indexOf("\"#", valueStart);
            if (valueStart == -1) {
                return oldContent;
            }
            StringBuilder valueSb = new StringBuilder();
            for (int i = valueStart + 1; i < comment.length(); i++) {
                if (comment.charAt(i) == '"') {
                    break;
                } else {
                    valueSb.append(comment.charAt(i));
                }
            }

            //获取属性，此处只取 ：= 之间的值
            int attributeLastIndex = comment.lastIndexOf("=", valueStart);
            if (attributeLastIndex == -1) {
                return oldContent;
            }
            int attributeStartIndex = comment.lastIndexOf(":", attributeLastIndex);
            if (attributeStartIndex == -1) {
                return oldContent;
            }
            String attributeStr = comment.substring(attributeStartIndex, attributeLastIndex).trim();
            if (attributeStr.length() == 0) {
                return oldContent;
            }

            //替换
            oldContent = replaceContent(valueSb.toString(), oldContent, colors, attributeStr);
            //继续查找下一个值
            valueStart += valueSb.length() + 1;
        }
        return oldContent;
    }

    /**
     * 替换内容
     *
     * @param value
     * @param oldContent
     * @param colors
     * @param targetItem
     * @return
     */
    private StringBuilder replaceContent(String value, StringBuilder oldContent, List<Entity> colors, String targetItem) {
        if (value.length() > 0 &&
                !value.contains("@color") &&
                !(value.startsWith("@{") && value.endsWith("}")) &&
                pattern.matcher(value).find()) {
            //为空，或者已经有@color 或者是 databinding的样式，就不需要处理，反之需要处理
            List<Entity> queryList = Lists.newArrayList();
            queryList.addAll(entityList);
            queryList.addAll(colors);
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
                targetId = "color_" + value;
                //去除#，不然会报红
                targetId = targetId.replace("#", "");
                colors.add(new Entity(targetId, value));
            }
            int index = 0;
            while (index < oldContent.length() && index >= 0) {
                index = Util.getRightIndex(oldContent, value, targetItem, 0);
                if (index != -1) {
                    //说明找到对应的值 +2 是因为替换的是 "value"，而不是value
                    oldContent = oldContent.replace(index, index + value.length() + 2, "\"@color/" + targetId + "\"");
                    //继续查找下一个值
                    index += value.length();
                }
            }
        }
        return oldContent;
    }
}