/**
 * 
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.apdplat.word.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apdplat.word.dictionary.impl.DictionaryTrie;
import org.apdplat.word.recognition.PersonName;
import org.apdplat.word.util.AutoDetector;
import org.apdplat.word.util.ResourceLoader;
import org.apdplat.word.util.WordConfTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 词典工厂
 通过系统属性及配置文件指定词典实现类（dic.class）和词典文件（dic.path）
 指定方式一，编程指定（高优先级）：
      WordConfTools.set("dic.class", "org.apdplat.word.dictionary.impl.DictionaryTrie");
      WordConfTools.set("dic.path", "classpath:dic.txt");
 指定方式二，Java虚拟机启动参数（中优先级）：
      java -Ddic.class=org.apdplat.word.dictionary.impl.DictionaryTrie -Ddic.path=classpath:dic.txt
 指定方式三，配置文件指定（低优先级）：
      在类路径下的word.conf中指定配置信息
      dic.class=org.apdplat.word.dictionary.impl.DictionaryTrie
      dic.path=classpath:dic.txt
 如未指定，则默认使用词典实现类（org.apdplat.word.dictionary.impl.DictionaryTrie）和词典文件（类路径下的dic.txt）
 * @author 杨尚川
 */
public final class DictionaryFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryFactory.class);
    private DictionaryFactory(){}
    public static final Dictionary getDictionary(){
        return DictionaryHolder.DIC;
    }
    public static void reload(){
        DictionaryHolder.reload();
    }
    private static final class DictionaryHolder{
        private static final Dictionary DIC = constructDictionary();
        private static Dictionary constructDictionary(){  
            try{
                //选择词典实现，可以通过参数选择不同的实现
                String dicClass = WordConfTools.get("dic.class", "org.apdplat.word.dictionary.impl.TrieV4");
                LOGGER.info("dic.class="+dicClass);
                return (Dictionary)Class.forName(dicClass.trim()).newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                System.err.println("词典装载失败:"+ex.getMessage());
                throw new RuntimeException(ex);
            }
        }
        static{
            reload();
        }
        public static void reload(){
            AutoDetector.loadAndWatch(new ResourceLoader(){

                @Override
                public void clear() {
                    DIC.clear();
                }

                @Override
                public void load(List<String> lines) {
                    LOGGER.info("初始化词典");
                    int count=0;
                    for(String surname : PersonName.getSurnames()){
                        if(surname.length() == 2){
                            count++;
                            lines.add(surname);
                        }
                    }
                    LOGGER.info("将 "+count+" 个复姓加入词典");
                    Map<Integer, Integer> map = new HashMap<>();
                    for(String line : lines){
                        //加入词典
                        DIC.add(line);
                        //统计不同长度的词的数目
                        int len = line.length();
                        Integer value = map.get(len);
                        if(value==null){
                            value=1;
                        }else{
                            value++;
                        }
                        map.put(len, value);
                    }
                    showStatistics(map);
                    if(DIC instanceof DictionaryTrie){
                        DictionaryTrie dictionaryTrie = (DictionaryTrie)DIC;
                        dictionaryTrie.showConflict();
                    }                    
                    LOGGER.info("词典初始化完毕");
                }

            }, WordConfTools.get("dic.path", "classpath:dic.txt")+","+WordConfTools.get("punctuation.path", "classpath:punctuation.txt"));
        }
        private static void showStatistics(Map<Integer, Integer> map) {
            //统计词数
            int wordCount=0;
            //统计平均词长
            int totalLength=0;
            for(int len : map.keySet()){
                totalLength += len * map.get(len);
                wordCount += map.get(len);
            }
            LOGGER.info("词数目："+wordCount+"，词典最大词长："+DIC.getMaxLength());
            for(int len : map.keySet()){
                if(len<10){
                    LOGGER.info("词长  "+len+" 的词数为："+map.get(len));
                }else{
                    LOGGER.info("词长 "+len+" 的词数为："+map.get(len));
                }
            }
            LOGGER.info("词典平均词长："+(float)totalLength/wordCount);
        }
    }
}