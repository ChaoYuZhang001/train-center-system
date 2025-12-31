package com.train.util;
import java.util.ArrayList;
import java.util.List;

public class QuestionSqlGenerator {

    // 获取30道题目内容列表
    private static List<String> getQuestionContents() {
        List<String> questions = new ArrayList<>();
        questions.add("202201（202201-A、202201-B、202201-C、202201-D、202201-E）：来自有反复流产史的35岁女性外周血淋巴细胞染色体。");
        questions.add("202507（202507-A、202507-B、202507-C、202507-D、202507-E）：来自生殖中心行PGD治疗的27岁孕妇，孕18周的胎儿羊水细胞染色体。");
        questions.add("202508（202508-A、202508-B、202508-C、202508-D、202508-E）：来自29岁孕妇孕17周的胎儿羊水细胞染色体，NIPT提示胎儿染色体异常。");
        questions.add("202509（202509-A、202509-B、202509-C、202509-D、202509-E）：来自8岁患儿的外周血淋巴细胞染色体，该患儿1年前发现生长速度缓慢，较同年龄儿童慢，近1年来年身高增长5cm/年；身材矮小，要求染色体核型分析检测。");
        questions.add("202510（202510-A、202510-B、202510-C、202510-D、202510-E、202510-F）：来自33岁女性外周血淋巴细胞染色体，因婚后10年未孕就诊。");
        questions.add("来自子代SNP-Array结果异常的女性外周血淋巴细胞染色体。");
        questions.add("来自既往2次自然流产史的孕妇孕23周的胎儿羊水细胞染色体。");
        questions.add("来自36岁高龄孕妇，孕18周的胎儿羊水细胞染色体。");
        questions.add("来自有一次流产史的梅毒病原携带孕妇，体外受精胚胎移植术后30周的胎儿脐血淋巴细胞染色体。胎儿生长发育迟缓，父母双方染色体未见异常。");
        questions.add("来自生殖中心行PGD治疗的36岁孕妇，孕18周的胎儿羊水细胞染色体。");
        questions.add("来自35岁高龄孕妇，孕17周的胎儿羊水细胞染色体。");
        questions.add("来自既往2次自然流产史的高龄孕妇孕20周的胎儿羊水细胞染色体。");
        questions.add("来自一孕20周胎儿羊水染色体，因孕妇42岁高龄行产前诊断。");
        questions.add("202409：来自37岁孕妇孕22周，超声提示结构异常的胎儿羊水细胞染色体。");
        questions.add("来自34岁女性外周血淋巴细胞染色体；婚后未避孕未孕8月，要求优生检查。");
        questions.add("来自产前筛查提示21三体高风险的孕妇孕21周的胎儿羊水细胞染色体。");
        questions.add("来自有过畸形儿分娩史的女性外周血淋巴细胞染色体。");
        questions.add("来自32岁婚后两年未育男性的外周血淋巴细胞染色体。");
        questions.add("来自孕17+5周的胎儿羊水细胞染色体，B超结果提示胎儿鼻骨未见显示，左心室强光斑。");
        questions.add("来自胎儿血清学筛查高危的28岁孕妇孕20周的胎儿羊水细胞染色体。");
        questions.add("202306（202306-A、202306-B、202306-C、202306-D、202306-E、202306-F）：来自有反复流产史的32岁孕妇22孕周的胎儿羊水细胞染色体。");
        questions.add("202307（202307-A、202307-B、202307-C、202307-D、202307-E）：来自32岁孕妇孕36周的胎儿脐血淋巴细胞染色体，超声显示胎儿NT增厚，侧脑室增宽。");
        questions.add("202308（202308-A、202308-B、202308-C、202308-D、202308-E）：来自NIPT结果异常的20岁孕妇孕17周的胎儿羊水细胞染色体。");
        questions.add("202309（202309-A、202309-B、202309-C、202309-D、202309-E）：来自拟行试管婴儿的30岁男子外周血淋巴细胞染色体，其妻子多次流产。");
        questions.add("202310（202310-A、202310-B、202310-C、202310-D、202310-E、202310-F）：来自无创提示18三体高风险的孕妇孕20周的胎儿羊水细胞染色体。");
        questions.add("202301（202301-A、202301-B、202301-C、202301-D、202301-E）：来自32岁孕妇孕17周的胎儿羊水细胞染色体，其父亲染色体核型分析提示异常，胎儿SNP-Array结果：arr[GRCh37]13q12.12(23473290_24908368) x3。");
        questions.add("202302（202302-A、202302-B、202302-C、202302-D、202302-E）：来自11岁运动发育迟缓儿童的外周血淋巴细胞染色体。");
        questions.add("202303（202303-A、202303-B、202303-C、202303-D、202303-E）：来自孕23周高龄孕妇的羊水细胞染色体。");
        questions.add("202304（202304-A、202304-B、202304-C、202304-D、202304-E、202304-F、202304-G、202304-H、202304-I）：来自33岁不孕不育的男性外周血淋巴细胞染色体。");
        questions.add("202305（202305-A、202305-B、202305-C、202305-D、202305-E、202305-F）：来自孕21周孕妇的羊水细胞染色体，胎儿超声检查提示\"心脏强光斑\"。");
        return questions;
    }

    // 获取30道题目对应的图片路径列表
    private static List<String> getQuestionImgPaths() {
        List<String> imgPaths = new ArrayList<>();
        imgPaths.add("[img1.png,img2.png,img3.png,img4.png,img5.png]");
        imgPaths.add("[img6.png,img7.png,img8.png,img9.png,img10.png]");
        imgPaths.add("[img11.png,img12.png,img13.png,img14.png,img15.png]");
        imgPaths.add("[img16.png,img17.png,img18.png,img19.png,img20.png]");
        imgPaths.add("[img21.png,img22.png,img23.png,img24.png,img25.png]");
        imgPaths.add("[img26.png,img27.png,img28.png,img29.png,img30.png]");
        imgPaths.add("[img31.png,img32.png,img33.png,img34.png,img35.png]");
        imgPaths.add("[img36.png,img37.png,img38.png,img39.png,img40.png]");
        imgPaths.add("[img41.png,img42.png,img43.png,img44.png,img45.png]");
        imgPaths.add("[img46.png,img47.png,img48.png,img49.png,img50.png]");
        imgPaths.add("[img51.png,img52.png,img53.png,img54.png,img55.png]");
        imgPaths.add("[img56.png,img57.png,img58.png,img59.png,img60.png,img61.png,img62.png]");
        imgPaths.add("[img63.png,img64.png,img65.png,img66.png,img67.png]");
        imgPaths.add("[img68.png,img69.png,img70.png,img71.png,img72.png]");
        imgPaths.add("[img73.png,img74.png,img75.png,img76.png,img77.png,img78.png]");
        imgPaths.add("[img79.png,img80.png,img81.png,img82.png,img83.png]");
        imgPaths.add("[img84.png,img85.png,img86.png,img87.png,img88.png]");
        imgPaths.add("[img89.png,img90.png,img91.png,img92.png,img93.png,img94.png]");
        imgPaths.add("[img95.png,img96.png,img97.png,img98.png,img99.png,img100.png]");
        imgPaths.add("[img101.png,img102.png,img103.png,img104.png,img105.png]");
        imgPaths.add("[img106.png,img107.png,img108.png,img109.png,img110.png,img111.png]");
        imgPaths.add("[img112.png,img113.png,img114.png,img115.png,img116.png]");
        imgPaths.add("[img117.png,img118.png,img119.png,img120.png,img121.png]");
        imgPaths.add("[img122.png,img123.png,img124.png,img125.png,img126.png]");
        imgPaths.add("[img127.png,img128.png,img129.png,img130.png,img131.png,img132.png]");
        imgPaths.add("[img133.png,img134.png,img135.png,img136.png,img137.png]");
        imgPaths.add("[img138.png,img139.png,img140.png,img141.png,img142.png]");
        imgPaths.add("[img143.png,img144.png,img145.png,img146.png,img147.png]");
        imgPaths.add("[img148.png,img149.png,img150.png,img151.png,img152.png,img153.png,img154.png,img155.png,img156.png]");
        imgPaths.add("[img157.png,img158.png,img159.png,img160.png,img161.png,img162.png]");
        return imgPaths;
    }

    // 获取30道题目答案列表
    private static List<String> getQuestionAnswers() {
        List<String> answers = new ArrayList<>();
        answers.add("46,XX,t(4;21)(p15.2;q22)");
        answers.add("46,XY,t(2;5)(q11.2;p13)");
        answers.add("46,XX,del(8)(p23");
        answers.add("46,XX");
        answers.add("46,XX,inv(5)(q15q32)");
        answers.add("46,XX");
        answers.add("46,XX,t(1;19)(q42.1;q13.4)");
        answers.add("46,XX,t(5;18)(q11.2;q21.3)");
        answers.add("46,XX,del(4)(p15.3)dn");
        answers.add("46,XY,t(17;22)(q21;q13)");
        answers.add("46,XX,inv(1)(p12q21)||46,XX,inv(1)(p13q21）");
        answers.add("46,XY,t(4;20)(q12;q11.2)");
        answers.add("47,XXY");
        answers.add("46,XY");
        answers.add("45,XX,psu dic(10;15)(p15;p11.2)");
        answers.add("46,XY,t(1;2)(p32;q21)");
        answers.add("46,XX");
        answers.add("46,XY,inv(9)(q22q33)");
        answers.add("47,XX,+21");
        answers.add("46,XX,t(8;9)(q24.3;p22)");
        answers.add("46,XY,t(1;11)(p36.2;p13)");
        answers.add("46,XY,t(7;15)(q11.2;q22)");
        answers.add("46,XY,del(5)(q23.1q23.3)");
        answers.add("46,XY");
        answers.add("46,XY,t(2;14)(q21;q13)");
        answers.add("46,XY,t(6;8)(p21.1;q24.1)");
        answers.add("46,XY,inv(2)(p13q31)");
        answers.add("46,XX");
        answers.add("46,XY,der(22)t(Y;22)(q12;p12)");
        answers.add("46,XY,t(4;7)(p15.2;p21)");
        return answers;
    }

    /**
     * SQL特殊字符转义方法（防止单引号导致SQL语法错误和注入风险）
     * @param content 待转义的字符串
     * @return 转义后的安全字符串
     */
    private static String escapeSql(String content) {
        if (content == null) {
            return "";
        }
        // 将单引号 ' 替换为两个单引号 '' （PostgreSQL/MySQL等主流数据库的转义方式）
        return content.replace("'","''");
    }

    /**
     * 生成批量SQL插入语句（支持单条打印或批量拼接）
     */
    public static void generateInsertSql() {
        List<String> questions = getQuestionContents();
        List<String> imgPaths = getQuestionImgPaths();
        List<String> answers = getQuestionAnswers();

        // 校验三个列表长度一致，避免数据不匹配
        if (questions.size() != imgPaths.size() || questions.size() != answers.size()) {
            System.out.println("错误：题目、图片、答案列表长度不一致！");
            System.out.println("题目数量：" + questions.size());
            System.out.println("图片数量：" + imgPaths.size());
            System.out.println("答案数量：" + answers.size());
            return;
        }

        // 方式1：生成单条SQL语句（便于调试，直接复制执行）
        System.out.println("========== 单条SQL插入语句（共" + questions.size() + "条）==========");
        for (int i = 0; i < questions.size(); i++) {
            String question = escapeSql(questions.get(i));
            String imgPath = escapeSql(imgPaths.get(i));
            String answer = escapeSql(answers.get(i));
            // 拼接单条SQL
            String singleSql = String.format(
                    "INSERT INTO public.train_question (question_content,question_img,answer,org_id,status,create_time) " +
                            "VALUES ('%s','%s','%s','0',1,now());",
                    question,imgPath,answer
            );
            // 打印第i+1题的SQL（便于对应题目编号）
            System.out.println("第" + (i + 1) + "题SQL：");
            System.out.println(singleSql);
            System.out.println(); // 空行分隔，便于阅读
        }

        // 方式2：生成批量SQL插入语句（提高入库效率，仅PostgreSQL支持VALUES后多组括号）
        System.out.println("========== 批量SQL插入语句（一次性执行）==========");
        StringBuilder batchSql = new StringBuilder();
        batchSql.append("INSERT INTO public.train_question (question_content,question_img,answer,org_id,status,create_time) VALUES ");
        for (int i = 0; i < questions.size(); i++) {
            String question = escapeSql(questions.get(i));
            String imgPath = escapeSql(imgPaths.get(i));
            String answer = escapeSql(answers.get(i));
            // 拼接每组值
            batchSql.append(String.format("('%s','%s','%s','0',1,now())",question,imgPath,answer));
            // 最后一组不加逗号，其余加逗号分隔
            if (i != questions.size() - 1) {
                batchSql.append(",");
            }
        }
        batchSql.append(";");
        System.out.println(batchSql.toString());
    }

    // 主方法：运行即可生成SQL
    public static void main(String[] args) {
        generateInsertSql();
    }
}