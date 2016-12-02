import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.builders.GlobalDocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.global.AutoColorCorrelogram;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.global.FCTH;
import net.semanticmetadata.lire.imageanalysis.features.global.JCD;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;

import com.searchengine.util.Configuration;
import com.searchengine.util.Contance;

public class Indexer {
	// 商品的最大ID
	private int maxGid = 0;

	// 最后记录的商品ID
	private int lastGid = 0;

	// 数据库链接
	private Connection Conn = null;

	// 是否重建索引
	private boolean reBuild = false;

	// 每次从数据库拉取的商品条数
	private int size = 1000;

	// 配置
	private Configuration cfg;

	// 获取商品的SQL
	private String getGoodsSql = "select id,pic_url,category_id from goods where category_id = ? and id > ? and id <= ? order by id asc limit ?,?";

	private GlobalDocumentBuilder globalDocumentBuilder;

	private int lenght = 0;

	public static void main(String[] args) throws IOException {
		Indexer indexer = new Indexer(args);
		// indexer.debug("maxGid="+indexer.maxGid+",lastGid="+indexer.lastGid);
		// 开始
		System.out.println("Start At "
				+ (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
						.format(new Date()));
		List<Integer> cids = indexer.getCatIds();
		for (int i = 0; i < cids.size(); i++) {
			List<Map<String, String>> goods;
			int p = 0;
			int cid = cids.get(i);
			do {
				goods = indexer.getGoods(cid, p++, indexer.size);
				indexer.addDoc(cid, goods);
			} while (goods.size() > 0);
		}
		System.out.println("End at "
				+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.format(new Date()));
		indexer.finished();
	}

	public Indexer(String args[]) {
		this.cfg = new Configuration(getPath() + "/config.properties");
		this.Conn = this.connection();
		this.lastGid = this.getLastGid();
		if ((args.length > 0 && args[0].equals("rotate")) || this.lastGid == 0) {
			this.delIndexFile();
			this.lastGid = 0;
			this.reBuild = true;
		}
		this.maxGid = this.getMaxGid();
		indexConfig();
	}

	private Connection connection() {
		Connection conn = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String dburl = "jdbc:" + this.cfg.getValue("db.dsn");
			conn = DriverManager.getConnection(dburl,
					this.cfg.getValue("db.user"), this.cfg.getValue("db.pawd"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}

	/**
	 * 获取所有分类
	 * 
	 * @return List
	 */
	private List<Integer> getCatIds() {
		List<Integer> cats = new ArrayList<Integer>();
		try {
			Statement st = this.Conn.createStatement();
			ResultSet rs = st
					.executeQuery("select id from category where display=1 and is_parent=0 order by id asc");
			while (rs.next()) {
				int cat_id = rs.getInt("id");
				cats.add(cat_id);
			}
		} catch (SQLException e) {
			System.out.println("获取分类失败：" + e.getMessage());
		}
		return cats;
	}

	private List<Map<String, String>> getGoods(int cid, int page, int size) {
		PreparedStatement ps;
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		try {
			ps = this.Conn.prepareStatement(this.getGoodsSql);
			ps.setInt(1, cid);
			ps.setInt(2, lastGid);
			ps.setInt(3, maxGid);
			ps.setInt(4, page * size);
			ps.setInt(5, size);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("goods_id", rs.getString("id"));
				map.put("pic_url", rs.getString("pic_url"));
				list.add(map);
			}
		} catch (SQLException e) {
			System.out.println("获取商品失败：" + cid + ":" + e.getMessage());
		}
		return list;
	}

	private int getLastGid() {
		int lastGid = 0;
		try {
			Statement st = this.Conn.createStatement();
			// this.debug(st.toString());
			ResultSet rs = st
					.executeQuery("select * from recore where `key`=1 limit 1");
			rs.next();
			lastGid = rs.getInt("last_gid");
		} catch (SQLException e) {
			System.out.println("获取最后记录商品ID失败：" + e.getMessage());
		}
		return lastGid;
	}

	private int getMaxGid() {
		int maxGid = 0;
		try {
			Statement st = this.Conn.createStatement();
			ResultSet rs = st.executeQuery("select max(id) maxid from goods");
			rs.next();
			maxGid = rs.getInt("maxid");
		} catch (SQLException e) {
			System.out.println("获取最大商品ID：" + e.getMessage());
		}
		return maxGid;
	}

	protected String getIndexPath(int catId) {
		return getPath() + "/" + (this.reBuild ? "index2/" : "index/") + catId;
	}

	protected String getPath() {
		return Contance.MAIN_PATH;
	}

	protected String pathTag() {
		return this.isWindows() ? "\\" : "/";
	}

	private void indexConfig() {
		globalDocumentBuilder = new GlobalDocumentBuilder(CEDD.class);
		globalDocumentBuilder.addExtractor(FCTH.class);
		globalDocumentBuilder.addExtractor(AutoColorCorrelogram.class);
		globalDocumentBuilder.addExtractor(JCD.class);

	}

	private IndexWriter openIndexWriter(int cid) {
		String indexPath = getIndexPath(cid);
		try {
			return LuceneUtils.createIndexWriter(indexPath, true,
					LuceneUtils.AnalyzerType.WhitespaceAnalyzer);
		} catch (IOException e) {
			System.out.println("writer is fault open");
			return null;
		}
	}

	private void closeIndexWriter(IndexWriter iw) {
		if (iw != null) {
			try {
				LuceneUtils.closeWriter(iw);
			} catch (IOException e) {
				System.out.println("writer is fault close");
			}
		}
	}

	protected void addDoc(int catId, List<Map<String, String>> imgs) {
		if (imgs.size() == 0) {
			return;
		}

		if (globalDocumentBuilder == null) {
			indexConfig();
		}

		File lockFile = new File(new StringBuilder(getIndexPath(catId))
				.append(pathTag()).append("write.lock").toString());
		if (lockFile.exists()) {
			lockFile.delete();
		}
		IndexWriter iw = openIndexWriter(catId);

		for (int i = 0; i < imgs.size(); i++) {
			Map<String, String> img = imgs.get(i);
			String goodsId = img.get("goods_id");
			String picUrl = img.get("pic_url");
			if (picUrl == "" || picUrl == null || picUrl.equals("")) {
				continue;
			}

			try {
				URL purl = new URL(picUrl);
				HttpURLConnection conn = (HttpURLConnection) purl
						.openConnection();
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(10000);
				conn.connect();
				if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
					System.out.println(new StringBuilder("fail:")
							.append(goodsId + ""));
					continue;
				}
				if (conn.getContentLength() == 0) {
					continue;
				}
				BufferedImage image = ImageIO.read(conn.getInputStream());
				Document document = globalDocumentBuilder.createDocument(image,
						String.format("%s,%s", goodsId, picUrl));
				iw.addDocument(document);
				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("cid:goodsid==========>" + catId + ":" + goodsId
					+ "=====>" + lenght++);
		}
		closeIndexWriter(iw);
	}

	protected void finished() {
		try {
			Statement st = this.Conn.createStatement();
			st.executeUpdate("replace into recore set last_gid=" + this.maxGid
					+ ",`key`=1");
			this.Conn.close();
			if (this.reBuild) {
				moveIndexFile();
			}
			System.out.println("Create success");
		} catch (SQLException e) {
			System.out.println("recore fail of finished：" + e.getMessage());
		}
	}

	protected void delIndexFile() {
		String indexPath = this.getPath() + "/index2";
		// this.debug(indexPath);
		String cmd = this.isWindows() ? "rd /s /q " + indexPath : "rm -rf "
				+ indexPath;
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			System.out.println("delete index file fail." + e.getMessage());
		}
	}

	protected void moveIndexFile() {
		String path = getPath(), old1 = path + "/index", old2 = path
				+ "/index2", new1 = path + "/index_old", re_cmd = this
				.isWindows() ? "ren" : "mv", rm_cmd = this.isWindows() ? "rd"
				: "rm -rf", cmd1 = re_cmd + " " + old1 + " " + new1, cmd2 = re_cmd
				+ " " + old2 + " " + old1, cmd3 = rm_cmd + " " + new1;
		Runtime run = Runtime.getRuntime();
		try {
			run.exec(cmd1);
			run.exec(cmd2);
			run.exec(cmd3);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			System.out.println("move index file fail." + e.getMessage());
		}
	}

	private void debug(String msg) {
		System.out.println(msg);
		System.exit(0);
	}

	private void debug(boolean msg) {
		System.out.println(msg);
		System.exit(0);
	}

	private boolean isWindows() {
		return System.getProperty("os.name").substring(0, 3).equals("Win");
	}

}
