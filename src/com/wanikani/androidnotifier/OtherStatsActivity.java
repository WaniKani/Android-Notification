package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.wanikani.androidnotifier.graph.HistogramChart;
import com.wanikani.androidnotifier.graph.HistogramPlot;
import com.wanikani.androidnotifier.graph.ProgressChart;
import com.wanikani.androidnotifier.graph.ProgressPlot;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.ItemsCache;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSLevel;
import com.wanikani.wklib.Vocabulary;

public class OtherStatsActivity extends Activity {
	
	private enum Job {
		
		BASE, OTHER
		
	};
	
	private class RetryListener implements DialogInterface.OnClickListener {
		
		@Override
		public void onClick (DialogInterface ifc, int which)
		{
			refresh ();
		}		
	}

	private class CancelListener implements DialogInterface.OnClickListener {
		
		@Override
		public void onClick (DialogInterface ifc, int which)
		{
			finish ();
		}		
	}
	
	private class MoreListener implements View.OnClickListener {
		
		@Override
		public void onClick (View view)
		{
			view.setVisibility (View.GONE);
			showMore ();
		}
		
	}

	private class ItemListener {
		
		public void reset (int levels)
		{
			/* empty */
		}
		
		public void update ()
		{
			/* empty */
		}

		public void newRadical (ItemLibrary<Radical> radicals)
		{
			/* empty */
		}
		
		public void newKanji (ItemLibrary<Kanji> kanji)
		{
			/* empty */
		}

		public void newVocab (ItemLibrary<Vocabulary> vocabs)
		{
			/* empty */
		}
		
	}
	
	private class KanjiProgressChart extends ItemListener {
		
		ProgressChart.SubPlot plot;
		
		String library;
		
		EnumMap<SRSLevel, ProgressPlot.DataSet> slds;
		
		ProgressPlot.DataSet rds;
		
		public KanjiProgressChart (ProgressChart chart, String title, String library)
		{
			this.library = library;
			
			plot = chart.addData (title); 
		}
		
		public void reset (int levels)
		{
			Resources res;
			
			res = getResources ();

			slds = new EnumMap<SRSLevel, ProgressPlot.DataSet> (SRSLevel.class);
			
			slds.put (SRSLevel.APPRENTICE, 
					  new ProgressPlot.DataSet (getString (R.string.tag_apprentice), 
							  					res.getColor (R.color.apprentice), 0));
			slds.put (SRSLevel.GURU, 
					  new ProgressPlot.DataSet (getString (R.string.tag_guru), 
							  					res.getColor (R.color.guru), 0));
			slds.put (SRSLevel.MASTER, 
					  new ProgressPlot.DataSet (getString (R.string.tag_master), 
							  					res.getColor (R.color.master), 0));
			slds.put (SRSLevel.ENLIGHTEN, 
					  new ProgressPlot.DataSet (getString (R.string.tag_enlightened), 
							  					res.getColor (R.color.enlightened), 0));
			slds.put (SRSLevel.BURNED, 
					  new ProgressPlot.DataSet (getString (R.string.tag_burned), 
							  					res.getColor (R.color.burned), 0));
			
			rds = new ProgressPlot.DataSet (getString (R.string.tag_locked),
											res.getColor (R.color.locked), library.length ());
		}
		
		public void newKanji (ItemLibrary<Kanji> kanji)
		{
			for (Kanji k : kanji.list) {
				if (k.stats != null && library.contains (k.character)) {
					slds.get (k.stats.srs).value++;
					rds.value--;
				}
			}
		}

		public void update ()
		{
			List<ProgressPlot.DataSet> l;
			
			l = new Vector<ProgressPlot.DataSet> (slds.values ());
			l.add (rds);
			
			plot.setData (l);
		}
		
	}
	
	private class ItemDistribution extends ItemListener {

		/// The series
		private List<HistogramPlot.Series> series;

		/// The chart to be updated
		private HistogramChart chart;

		/// The SRS to series mapping
		private EnumMap<SRSLevel, HistogramPlot.Series> map;
		
		/// The SRS to idx mapping
		private EnumMap<SRSLevel, Integer> imap;

		/// The actual data
		private List<HistogramPlot.Samples> bars; 
		
		public ItemDistribution (Context ctxt, HistogramChart chart)
		{
			Resources res;

			this.chart = chart;
			
			res = ctxt.getResources ();
								
			series = new Vector<HistogramPlot.Series> ();
			map = new EnumMap<SRSLevel, HistogramPlot.Series> (SRSLevel.class);
			imap = new EnumMap<SRSLevel, Integer> (SRSLevel.class);
			bars = new Vector<HistogramPlot.Samples> ();
			
			add (res, SRSLevel.APPRENTICE, R.string.tag_apprentice, R.color.apprentice);
			add (res, SRSLevel.GURU, R.string.tag_guru, R.color.guru);
			add (res, SRSLevel.MASTER, R.string.tag_master, R.color.master);
			add (res, SRSLevel.ENLIGHTEN, R.string.tag_enlightened, R.color.enlightened);
			add (res, SRSLevel.BURNED, R.string.tag_burned, R.color.burned);
			
			imap.put (SRSLevel.APPRENTICE, 0);
			imap.put (SRSLevel.GURU, 1);
			imap.put (SRSLevel.MASTER, 2);
			imap.put (SRSLevel.ENLIGHTEN, 3);
			imap.put (SRSLevel.BURNED, 4);
		}
		
		public void reset (int levels)
		{
			initBars (levels);
		}
		
		public void update ()
		{
			chart.setData (series, bars, -1);
		}

		private void add (Resources res, SRSLevel level, int string, int color)		
		{			
			HistogramPlot.Series s;
			
			s = new HistogramPlot.Series (res.getString (string), res.getColor (color));
			series.add (s);
			map.put (level, s);
		}
		
		private void initBars (int levels)
		{
			HistogramPlot.Samples bar;
			int i;
			
			for (i = 1; i <= levels; i++) {
				bar = new HistogramPlot.Samples (Integer.toString (i));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.APPRENTICE)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.GURU)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.MASTER)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.ENLIGHTEN)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.BURNED)));
				bars.add (bar);
			}
		}
				
		public void newRadical (ItemLibrary<Radical> radicals)
		{
			for (Item i : radicals.list)
				put (i);
		}
		
		public void newKanji (ItemLibrary<Kanji> kanji)
		{
			for (Item i : kanji.list)
				put (i);
		}

		public void newVocab (ItemLibrary<Vocabulary> vocabs)
		{
			for (Item i : vocabs.list)
				put (i);			
		}

		private void put (Item i)
		{
			if (i.stats != null)	// stats is null for loced items
				bars.get (i.level - 1).samples.get (imap.get (i.stats.srs)).value++;
		}
						
	}

	/**
	 * The asynch task that loads all the info from WK, feeds the database and
	 * publishes the progress.
	 */
	private class Task extends AsyncTask<ItemListener, Integer, Boolean> {

		/// WK connection
		private Connection conn;
		
		/// The meter
		private Connection.Meter meter;
		
		/// Must reset
		private boolean reset;
		
		/// List of item types to load
		private EnumSet<Item.Type>types;
		
		/// Progress bar
		private ProgressBar pb;
		
		/// Number of levels to load at once
		private static final int BUNCH_SIZE = 5;
		
		/// The job type
		private Job job;
		
		public Task (Job job, Connection.Meter meter, Connection conn, Context ctxt, 
					 ProgressBar pb, boolean reset, Item.Type...types)
		{
			int i;
			
			this.job = job;
			this.conn = conn;
			this.meter = meter;
			this.pb = pb;
			this.reset = reset;
			
			this.types = EnumSet.noneOf (Item.Type.class);
			
			for (i = 0; i < types.length; i++)
				this.types.add (types [i]);
		}
				
		/**
		 * The reconstruction process itself. It opens a DB reconstruction object,
		 * loads all the items, and retrieves the new core stats 
		 * @return true if everything is ok
		 */
		@Override
		protected Boolean doInBackground (ItemListener... listeners)
		{
			ItemLibrary<Radical> rlib;
			ItemLibrary<Kanji> klib;
			ItemLibrary<Vocabulary> vlib;
			int i, j, levels, bunch [];

			try {
				levels = conn.getUserInformation (meter).level;
			} catch (IOException e) {
				return false;
			}

			publishProgress ((100 * 1) / (levels + 2));

			if (reset)
				for (ItemListener l : listeners)
					l.reset (levels);				
			
			try {
				if (types.contains (Item.Type.RADICAL)) {
					rlib = conn.getRadicals (meter);
					for (ItemListener l : listeners)
						l.newRadical (rlib);
				}
			} catch (IOException e) {
				return false;
			} 

			try {
				if (types.contains (Item.Type.KANJI)) {
					klib = conn.getKanji (meter);
					for (ItemListener l : listeners)
						l.newKanji (klib);
				}
			} catch (IOException e) {
				return false;
			} 

			publishProgress ((100 * 2) / (levels + 2));
			
			try {
				if (types.contains (Item.Type.VOCABULARY)) {
					i = 1;
					while (i <= levels) {
						bunch = new int [Math.min (BUNCH_SIZE, levels - i + 1)];
						for (j = 0; j < BUNCH_SIZE && i <= levels; j++)
							bunch [j] = i++;
						vlib = conn.getVocabulary (meter, bunch);
						for (ItemListener l : listeners)
							l.newVocab (vlib);
						publishProgress ((100 * (i - 1)) / (levels + 2));
					}
				}
			} catch (IOException e) {
				return false;
			} 

			return true;
		}	
				
		@Override
		protected void onProgressUpdate (Integer... i)
		{
			if (pb != null)
				pb.setProgress (i [i.length - 1]);
		}
		
		/**
		 * Ends the reconstruction process by telling everybody how it went.
		 * @param ok if everything was ok
		 */
		@Override
		protected void onPostExecute (Boolean ok)
		{
			completed (job, ok);
		}
	}

	private Connection conn;
	
	private ItemsCache cache;
	
	private static final String PREFIX = OtherStatsActivity.class.toString () + ".";
	
	public static final String EXTRA_CACHE = PREFIX + "CACHE";
	
	private List<ItemListener> listeners;
	
	private List<ItemListener> olisteners;

	private ProgressBar spinner;
	
	private ProgressBar opb;
	
	private ProgressChart jlptChart;

	private ProgressChart joyoChart;
	
	private HistogramChart levelsChart;
	
	private HistogramChart kanjiLevelsChart;
	
	private ItemDistribution itemd;
	
	private ItemDistribution kanjid;

	private View panel;
	
	private boolean visible;	
	
	private static final String KLIB_JLPT_1 =
		"氏統保第結派案策基価提挙応企検藤沢裁証援施井護展態鮮視条幹独宮率衛張監環審義訴株姿閣衆評影松撃佐核整融製票渉響推請器士討攻崎督授催及憲離激摘系批郎健盟従修隊織拡故振弁就異献厳維浜遺塁邦素遣抗模雄益緊標" +
		"宣昭廃伊江僚吉盛皇臨踏壊債興源儀創障継筋闘葬避司康善逮迫惑崩紀聴脱級博締救執房撤削密措志載陣我為抑幕染奈傷択秀徴弾償功拠秘拒刑塚致繰尾描鈴盤項喪伴養懸街契掲躍棄邸縮還属慮枠恵露沖緩節需射購揮充貢鹿却端" +
		"賃獲郡併徹貴衝焦奪災浦析譲称納樹挑誘紛至宗促慎控智握宙俊銭渋銃操携診託撮誕侵括謝駆透津壁稲仮裂敏是排裕堅訳芝綱典賀扱顧弘看訟戒祉誉歓奏勧騒閥甲縄郷揺免既薦隣華範隠徳哲杉釈己妥威豪熊滞微隆症暫忠倉彦肝喚" +
		"沿妙唱阿索誠襲懇俳柄驚麻李浩剤瀬趣陥斎貫仙慰序旬兼聖旨即柳舎偽較覇詳抵脅茂犠旗距雅飾網竜詩繁翼潟敵魅嫌斉敷擁圏酸罰滅礎腐脚潮梅尽僕桜滑孤炎賠句鋼頑鎖彩摩励縦輝蓄軸巡稼瞬砲噴誇祥牲秩帝宏唆阻泰賄撲堀菊絞" +
		"縁唯膨矢耐塾漏慶猛芳懲剣彰棋丁恒揚冒之倫陳憶潜梨仁克岳概拘墓黙須偏雰遇諮狭卓亀糧簿炉牧殊殖艦輩穴奇慢鶴謀暖昌拍朗寛覆胞泣隔浄没暇肺貞靖鑑飼陰銘随烈尋稿丹啓也丘棟壌漫玄粘悟舗妊熟旭恩騰往豆遂狂岐陛緯培衰" +
		"艇屈径淡抽披廷錦准暑磯奨浸剰胆繊駒虚霊帳悔諭惨虐翻墜沼据肥徐糖搭盾脈滝軌俵妨擦鯨荘諾雷漂懐勘栽拐駄添冠斜鏡聡浪亜覧詐壇勲魔酬紫曙紋卸奮欄逸涯拓眼獄尚彫穏顕巧矛垣欺釣萩粛栗愚嘉遭架鬼庶稚滋幻煮姫誓把践呈" +
		"疎仰剛疾征砕謡嫁謙后嘆菌鎌巣頻琴班棚潔酷宰廊寂辰霞伏碁俗漠邪晶墨鎮洞履劣那殴娠奉憂朴亭淳怪鳩酔惜穫佳潤悼乏該赴桑桂髄虎盆晋穂壮堤飢傍疫累痴搬晃癒桐寸郭尿凶吐宴鷹賓虜陶鐘憾猪紘磁弥昆粗訂芽庄傘敦騎寧循忍" +
		"怠如寮祐鵬鉛珠凝苗獣哀跳匠垂蛇澄縫僧眺亘呉凡憩媛溝恭刈睡錯伯笹穀陵霧魂弊妃舶餓窮掌麗綾臭悦刃縛暦宜盲粋辱毅轄猿弦稔窒炊洪摂飽冗桃狩朱渦紳枢碑鍛刀鼓裸猶塊旋弓幣膜扇腸槽慈楊伐駿漬糾亮墳坪紺娯椿舌羅峡俸厘" +
		"峰圭醸蓮弔乙汁尼遍衡薫猟羊款閲偵喝敢胎酵憤豚遮扉硫赦窃泡瑞又慨紡恨肪扶戯伍忌濁奔斗蘭迅肖鉢朽殻享秦茅藩沙輔媒鶏禅嘱胴迭挿嵐椎絹陪剖譜郁悠淑帆暁傑楠笛玲奴錠拳翔遷拙侍尺峠篤肇渇叔雌亨堪叙酢吟逓嶺甚喬崇漆" +
		"岬癖愉寅礁乃洲屯樺槙姻巌擬塀唇睦閑胡幽峻曹詠卑侮鋳抹尉槻隷禍蝶酪茎帥逝汽琢匿襟蛍蕉寡琉痢庸朋坑藍賊搾畔遼唄孔橘漱呂拷嬢苑巽杜渓翁廉謹瞳湧欣窯褒醜升殉煩巴禎劾堕租稜桟倭婿慕斐罷矯某囚魁虹鴻泌於赳漸蚊葵厄" +
		"藻禄孟嫡尭嚇巳凸暢韻霜硝勅芹杏棺儒鳳馨慧愁楼彬匡眉欽薪褐賜嵯綜繕栓翠鮎榛凹艶惣蔦錬隼渚衷逐斥稀芙詔皐雛惟佑耀黛渥憧宵妄惇脩甫酌蚕嬉蒼暉頒只肢檀凱彗謄梓丑嗣叶汐絢朔伽畝抄爽黎惰蛮冴旺萌偲壱瑠允侯蒔鯉弧遥" +
		"舜瑛附彪卯但綺芋茜凌皓洸毬婆緋鯛怜邑倣碧啄穣酉悌倹柚繭亦詢采紗賦眸玖弐錘諄倖痘笙侃裟洵爾耗昴銑莞伶碩宥滉晏伎朕迪綸且竣晨吏燦麿頌箇楓琳梧哉澪匁晟衿凪梢丙颯茄勺恕蕗瑚遵瞭燎虞柊侑謁斤嵩捺蓉茉袈燿誼冶栞墾" +
		"勁菖旦椋叡紬胤凜亥爵脹麟莉汰瑶瑳耶椰絃丞璃奎塑昂柾熙菫諒鞠崚濫捷";

	private static final String KLIB_JLPT_2 =
		"党協総区領県設改府査委軍団各島革村勢減再税営比防補境導副算輸述線農州武象域額欧担準賞辺造被技低復移個門課脳極含蔵量型況針専谷史階管兵接細効丸湾録省旧橋岸周材戸央券編捜竹超並療採森競介根販歴将幅般貿講林" +
		"装諸劇河航鉄児禁印逆換久短油暴輪占植清倍均億圧芸署伸停爆陸玉波帯延羽固則乱普測豊厚齢囲卒略承順岩練軽了庁城患層版令角絡損募裏仏績築貨混昇池血温季星永著誌庫刊像香坂底布寺宇巨震希触依籍汚枚複郵仲栄札板骨" +
		"傾届巻燃跡包駐弱紹雇替預焼簡章臓律贈照薄群秒奥詰双刺純翌快片敬悩泉皮漁荒貯硬埋柱祭袋筆訓浴童宝封胸砂塩賢腕兆床毛緑尊祝柔殿濃液衣肩零幼荷泊黄甘臣浅掃雲掘捨軟沈凍乳恋紅郊腰炭踊冊勇械菜珍卵湖喫干虫刷湯溶" +
		"鉱涙匹孫鋭枝塗軒毒叫拝氷乾棒祈拾粉糸綿汗銅湿瓶咲召缶隻脂蒸肌耕鈍泥隅灯辛磨麦姓筒鼻粒詞胃畳机膚濯塔沸灰菓帽枯涼舟貝符憎皿肯燥畜挟曇滴伺";
	
	private static final String KLIB_JLPT_3 =
		"政議民連対部合市内相定回選米実関決全表戦経最現調化当約首法性要制治務成期取都和機平加受続進数記初指権支産点報済活原共得解交資予向際勝面告反判認参利組信在件側任引求所次昨論官増係感情投示変打直両式確果容" +
		"必演歳争談能位置流格疑過局放常状球職与供役構割費付由説難優夫収断石違消神番規術備宅害配警育席訪乗残想声念助労例然限追商葉伝働形景落好退頭負渡失差末守若種美命福望非観察段横深申様財港識呼達良候程満敗値突" +
		"光路科積他処太客否師登易速存飛殺号単座破除完降責捕危給苦迎園具辞因馬愛富彼未舞亡冷適婦寄込顔類余王返妻背熱宿薬険頼覚船途許抜便留罪努精散静婚喜浮絶幸押倒等老曲払庭徒勤遅居雑招困欠更刻賛抱犯恐息遠戻願絵" +
		"越欲痛笑互束似列探逃遊迷夢君閉緒折草暮酒悲晴掛到寝暗盗吸陽御歯忘雪吹娘誤洗慣礼窓昔貧怒泳祖杯疲皆鳴腹煙眠怖耳頂箱晩寒髪忙才靴恥偶偉猫幾";
	
	private static final String KLIB_JLPT_4 =
		"会同事自社発者地業方新場員立開手力問代明動京目通言理体田主題意不作用度強公持野以思家世多正安院心界教文元重近考画海売知道集別物使品計死特私始朝運終台広住真有口少町料工建空急止送切転研足究楽起着店病質待" +
		"試族銀早映親験英医仕去味写字答夜音注帰古歌買悪図週室歩風紙黒花春赤青館屋色走秋夏習駅洋旅服夕借曜飲肉貸堂鳥飯勉冬昼茶弟牛魚兄犬妹姉漢";
	
	private static final String KLIB_JLPT_5 =
		"日一国人年大十二本中長出三時行見月後前生五間上東四今金九入学高円子外八六下来気小七山話女北午百書先名川千水半男西電校語土木聞食車何南万毎白天母火右読友左休父雨";
	
	private static final String KLIB_JOYO_1 =
		"一右雨円王音下火花貝学気九休玉金空月犬見五口校左三山子四糸字耳七車手十出女小上森人水正生青夕石赤千川先早草足村大男竹中虫町天田土二日入年白八百文木本名目立力林六";

	private static final String KLIB_JOYO_2 =
		"引羽雲園遠何科夏家歌画回会海絵外角楽活間丸岩顔汽記帰弓牛魚京強教近兄形計元言原戸古午後語工公広交光考行高黄合谷国黒今才細作算止市矢姉思紙寺自時室社弱首秋週春書少場色食心新親図数西声星晴切雪船線前組走多" +
		"太体台地池知茶昼長鳥朝直通弟店点電刀冬当東答頭同道読内南肉馬売買麦半番父風分聞米歩母方北毎妹万明鳴毛門夜野友用曜来里理話";
	
	private static final String KLIB_JOYO_3 =
		"悪安暗医委意育員院飲運泳駅央横屋温化荷界開階寒感漢館岸起期客究急級宮球去橋業曲局銀区苦具君係軽血決研県庫湖向幸港号根祭皿仕死使始指歯詩次事持式実写者主守取酒受州拾終習集住重宿所暑助昭消商章勝乗植申身神" +
		"真深進世整昔全相送想息速族他打対待代第題炭短談着注柱丁帳調追定庭笛鉄転都度投豆島湯登等動童農波配倍箱畑発反坂板皮悲美鼻筆氷表秒病品負部服福物平返勉放味命面問役薬由油有遊予羊洋葉陽様落流旅両緑礼列練路和";
	
	private static final String KLIB_JOYO_4 =
		"愛案以衣位囲胃印英栄塩億加果貨課芽改械害街各覚完官管関観願希季紀喜旗器機議求泣救給挙漁共協鏡競極訓軍郡径型景芸欠結建健験固功好候航康告差菜最材昨札刷殺察参産散残士氏史司試児治辞失借種周祝順初松笑唱焼象" +
		"照賞臣信成省清静席積折節説浅戦選然争倉巣束側続卒孫帯隊達単置仲貯兆腸低底停的典伝徒努灯堂働特得毒熱念敗梅博飯飛費必票標不夫付府副粉兵別辺変便包法望牧末満未脈民無約勇要養浴利陸良料量輪類令冷例歴連老労録";
	
	private static final String KLIB_JOYO_5 =
		"圧移因永営衛易益液演応往桜恩可仮価河過賀快解格確額刊幹慣眼基寄規技義逆久旧居許境均禁句群経潔件券険検限現減故個護効厚耕鉱構興講混査再災妻採際在財罪雑酸賛支志枝師資飼示似識質舎謝授修述術準序招承証条状常" +
		"情織職制性政勢精製税責績接設舌絶銭祖素総造像増則測属率損退貸態団断築張提程適敵統銅導徳独任燃能破犯判版比肥非備俵評貧布婦富武復複仏編弁保墓報豊防貿暴務夢迷綿輸余預容略留領";
	
	private static final String KLIB_JOYO_6 =
		"異遺域宇映延沿我灰拡革閣割株干巻看簡危机揮貴疑吸供胸郷勤筋系敬警劇激穴絹権憲源厳己呼誤后孝皇紅降鋼刻穀骨困砂座済裁策冊蚕至私姿視詞誌磁射捨尺若樹収宗就衆従縦縮熟純処署諸除将傷障城蒸針仁垂推寸盛聖誠宣専" +
		"泉洗染善奏窓創装層操蔵臓存尊宅担探誕段暖値宙忠著庁頂潮賃痛展討党糖届難乳認納脳派拝背肺俳班晩否批秘腹奮並陛閉片補暮宝訪亡忘棒枚幕密盟模訳郵優幼欲翌乱卵覧裏律臨朗論";
	
	private static final String KLIB_JOYO_S =
		"亜哀挨曖握扱宛嵐依威為畏尉萎偉椅彙違維慰緯壱逸茨芋咽姻淫陰隠韻唄鬱畝浦詠影鋭疫悦越謁閲炎怨宴媛援煙猿鉛縁艶汚凹押旺欧殴翁奥岡憶臆虞乙俺卸穏佳苛架華菓渦嫁暇禍靴寡箇稼蚊牙瓦雅餓介戒怪拐悔皆塊楷潰壊懐諧劾" +
		"崖涯慨蓋該概骸垣柿核殻郭較隔獲嚇穫岳顎掛潟括喝渇葛滑褐轄且釜鎌刈甘汗缶肝冠陥乾勘患貫喚堪換敢棺款閑勧寛歓監緩憾還環韓艦鑑含玩頑企伎岐忌奇祈軌既飢鬼亀幾棋棄毀畿輝騎宜偽欺儀戯擬犠菊吉喫詰却脚虐及丘朽臼糾" +
		"嗅窮巨拒拠虚距御凶叫狂享況峡挟狭恐恭脅矯響驚仰暁凝巾斤菌琴僅緊錦謹襟吟駆惧愚偶遇隅串屈掘窟熊繰勲薫刑茎契恵啓掲渓蛍傾携継詣慶憬稽憩鶏迎鯨隙撃桁傑肩倹兼剣拳軒圏堅嫌献遣賢謙鍵繭顕懸幻玄弦舷股虎孤弧枯雇誇" +
		"鼓錮顧互呉娯悟碁勾孔巧甲江坑抗攻更拘肯侯恒洪荒郊香貢控梗喉慌硬絞項溝綱酵稿衡購乞拷剛傲豪克酷獄駒込頃昆恨婚痕紺魂墾懇佐沙唆詐鎖挫采砕宰栽彩斎債催塞歳載埼剤崎削柵索酢搾錯咲刹拶撮擦桟惨傘斬暫旨伺刺祉肢施" +
		"恣脂紫嗣雌摯賜諮侍滋慈餌8璽鹿軸𠮟疾執湿嫉漆芝赦斜煮遮邪蛇酌釈爵寂朱狩殊珠腫趣寿呪需儒囚舟秀臭袖羞愁酬醜蹴襲汁充柔渋銃獣叔淑粛塾俊瞬旬巡盾准殉循潤遵庶緒如叙徐升召匠床抄肖尚昇沼宵症祥称渉紹訟掌晶焦硝粧" +
		"詔奨詳彰憧衝償礁鐘丈冗浄剰畳縄壌嬢錠譲醸拭殖飾触嘱辱尻伸芯辛侵津唇娠振浸紳診寝慎審震薪刃尽迅甚陣尋腎須吹炊帥粋衰酔遂睡穂随髄枢崇据杉裾瀬是井姓征斉牲凄逝婿誓請醒斥析脊隻惜戚跡籍拙窃摂仙占扇栓旋煎羨腺詮" +
		"践箋潜遷薦繊鮮禅漸膳繕狙阻租措粗疎訴塑遡8礎双壮荘捜挿桑掃曹曽爽喪痩葬僧遭槽踪燥霜騒藻憎贈即促捉俗賊遜8汰妥唾堕惰駄耐怠胎泰堆袋逮替滞戴滝択沢卓拓託濯諾濁但脱奪棚誰丹旦胆淡嘆端綻鍛弾壇恥致遅痴稚緻畜逐蓄" +
		"秩窒嫡沖抽衷酎鋳駐弔挑彫眺釣貼超跳徴嘲澄聴懲勅捗沈珍朕陳鎮椎墜塚漬坪爪鶴呈廷抵邸亭貞帝訂逓偵堤艇締諦泥摘滴溺迭哲徹撤添塡殿斗吐妬途渡塗賭奴怒到逃倒凍唐桃透悼盗陶塔搭棟痘筒稲踏謄藤闘騰洞胴瞳峠匿督篤栃凸" +
		"突屯豚頓貪鈍曇丼那奈梨謎鍋軟尼弐匂虹尿妊忍寧捻粘悩濃把覇婆罵杯排廃輩培陪媒賠伯拍泊迫剝舶薄漠縛爆箸肌鉢髪伐抜罰閥氾帆汎伴阪畔般販斑搬煩頒範繁藩蛮盤妃彼披卑疲被扉碑罷避尾眉微膝肘匹泌姫漂苗描猫浜賓頻敏瓶" +
		"扶怖阜附訃赴浮符普腐敷膚賦譜侮舞封伏幅覆払沸紛雰噴墳憤丙併柄塀幣弊蔽餅壁璧癖蔑偏遍哺捕舗募慕簿芳邦奉抱泡胞俸倣峰砲崩蜂飽褒縫乏忙坊妨房肪某冒剖紡傍帽貌膨謀頰朴睦僕墨撲没勃堀奔翻凡盆麻摩磨魔昧埋膜枕又抹" +
		"慢漫魅岬蜜妙眠矛霧娘冥銘滅免麺茂妄盲耗猛網黙紋冶弥厄躍闇喩愉諭癒唯幽悠湧猶裕雄誘憂融与誉妖庸揚揺溶腰瘍踊窯擁謡抑沃翼拉裸羅雷頼絡酪辣濫藍欄吏痢履璃離慄柳竜粒隆硫侶虜慮了涼猟陵僚寮療瞭糧厘倫隣瑠涙累塁励" +
		"戻鈴零霊隷齢麗暦劣烈裂恋廉錬呂炉賂露弄郎浪廊楼漏籠麓賄脇惑枠湾腕";
	
	@Override
	public void onCreate (Bundle bundle) 
	{		
		super.onCreate (bundle);

		setContentView (R.layout.other_stats);
		
		spinner = (ProgressBar) findViewById (R.id.pb_status);
		opb = (ProgressBar) findViewById (R.id.os_pb_other);
		panel = findViewById (R.id.os_panel);
		jlptChart = (ProgressChart) findViewById (R.id.os_jlpt);
		joyoChart = (ProgressChart) findViewById (R.id.os_joyo);
		kanjiLevelsChart = (HistogramChart) findViewById (R.id.os_kanji_levels);
		levelsChart = (HistogramChart) findViewById (R.id.os_levels);
		levelsChart.setVisibility (View.GONE);

		conn = new Connection (SettingsActivity.getLogin (this));
		if (cache == null)
			cache = new ItemsCache ();
		
		listeners = new Vector<ItemListener> ();		
		olisteners = new Vector<ItemListener> ();		
		
		addKanjiListener (jlptChart, R.string.jlpt5, KLIB_JLPT_5);		
		addKanjiListener (jlptChart, R.string.jlpt4, KLIB_JLPT_4);		
		addKanjiListener (jlptChart, R.string.jlpt3, KLIB_JLPT_3);		
		addKanjiListener (jlptChart, R.string.jlpt2, KLIB_JLPT_2);		
		addKanjiListener (jlptChart, R.string.jlpt1, KLIB_JLPT_1);
		
		addKanjiListener (joyoChart, R.string.joyo1, KLIB_JOYO_1);
		addKanjiListener (joyoChart, R.string.joyo2, KLIB_JOYO_2);
		addKanjiListener (joyoChart, R.string.joyo3, KLIB_JOYO_3);
		addKanjiListener (joyoChart, R.string.joyo4, KLIB_JOYO_4);
		addKanjiListener (joyoChart, R.string.joyo5, KLIB_JOYO_5);
		addKanjiListener (joyoChart, R.string.joyo6, KLIB_JOYO_6);
		addKanjiListener (joyoChart, R.string.joyoS, KLIB_JOYO_S);

		kanjid = new ItemDistribution (this, kanjiLevelsChart);
		itemd = new ItemDistribution (this, levelsChart);
		
		listeners.add (kanjid);
		listeners.add (itemd);
		olisteners.add (itemd);

		refresh ();
	}
		
	@Override
	public void onNewIntent (Intent intent)
	{
		super.onNewIntent (intent);

		if (intent.hasExtra (EXTRA_CACHE))
			cache = (ItemsCache) intent.getSerializableExtra (EXTRA_CACHE);		
	}
	
	@Override
	public void onResume ()
	{
		super.onResume ();
		
		visible = true;
	}
	
	@Override
	public void onPause ()
	{
		super.onPause ();
		
		visible = false;
	}

	protected void addKanjiListener (ProgressChart chart, int id, String library)
	{
		listeners.add (new KanjiProgressChart (chart, getString (id), library));
	}
	
	protected void refresh ()
	{
		spinner.setVisibility (View.VISIBLE);
		new Task (Job.BASE, MeterSpec.T.MORE_STATS.get (this), conn, this, null, 
				  true, Item.Type.KANJI).execute (listeners.toArray (new ItemListener [0]));
	}
	
	protected void completed (Job job, boolean ok)
	{
		Button more;
		
		spinner.setVisibility (View.GONE);
		opb.setVisibility (View.GONE);
		
		if (ok) {
			panel.setVisibility (View.VISIBLE);
			for (ItemListener l : listeners)
				l.update ();
			
			if (job == Job.BASE) {
				more = (Button) findViewById (R.id.os_more);
				more.setVisibility (View.VISIBLE);
				more.setOnClickListener (new MoreListener ());
			}
		} else
			showErrorMessage ();
	}

	protected void showErrorMessage ()
	{
		AlertDialog.Builder builder;
		Dialog dialog;
		
		if (!visible)
			return;
		
		builder = new AlertDialog.Builder (this);
		builder.setTitle (R.string.other_stats_error_message_title);
		builder.setMessage (R.string.other_stats_error_message_text);
		builder.setPositiveButton (R.string.other_stats_message_retry, new RetryListener ());
		builder.setNegativeButton (R.string.other_stats_message_cancel, new CancelListener ());
		
		dialog = builder.create ();
		
		dialog.show ();		
	}
	
	protected void showMore ()
	{
		ScrollView sv;
		
		sv = (ScrollView) findViewById (R.id.os_scroll);
		opb.setVisibility (View.VISIBLE);
		new Task (Job.OTHER, MeterSpec.T.OTHER_STATS.get (this), conn, this, opb, false, 
				  Item.Type.RADICAL, Item.Type.VOCABULARY).execute (olisteners.toArray (new ItemListener [0]));
		levelsChart.spin (true);
		levelsChart.setVisibility (View.VISIBLE);
		sv.fullScroll (ScrollView.FOCUS_DOWN);
	}

}
