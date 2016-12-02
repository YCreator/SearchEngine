package com.searchengine.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.semanticmetadata.lire.aggregators.BOVW;
import net.semanticmetadata.lire.builders.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.global.AutoColorCorrelogram;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.global.FCTH;
import net.semanticmetadata.lire.imageanalysis.features.local.simple.SimpleExtractor;
import net.semanticmetadata.lire.searchers.GenericFastImageSearcher;
import net.semanticmetadata.lire.searchers.ImageSearchHits;
import net.semanticmetadata.lire.searchers.ImageSearcher;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import com.searchengine.util.Contance;

public class Search extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4330390234671179896L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		String url = req.getParameter("url");
		String method = req.getParameter("method");
		if (method == null) {
			method = "";
		}
		String cate = "0";
		//String indexPath = Contance.INDEX_PATH+"/"+cate;
		//String path = Contance.MAIN_PATH+"/img/"+url;
		String path = url;
		if (!url.contains("http") && !url.contains("https")) {
			path = "http://192.168.1.190:8080/ImageService/img/"+url;
		}
		resp.setContentType("text/html; charset=utf-8");
		PrintWriter out = resp.getWriter();
		out.println("<html>");                    //实现生成静态Html
        out.println("<head>");
        out.println("<meta http-equiv=\"Content-Type\"content=\"text/html;charset=utf-8\">");
        out.println("<title>DataBase Connection</title>");
        out.println("</head>");
        out.println("<body bgcolor=\"white\">");
        out.println("<center><span>");
        out.print("<img width=\"200px\" src=\""+path+"\" \\>");
        out.println("</span></center>");
        out.println("<h3>搜索结果</h3>");
        out.println("<hr style=\"height:1px;border:none;border-top:1px solid #555555;\" />");
	
		URL picUrl = new URL(path);
		HttpURLConnection conn = (HttpURLConnection) picUrl.openConnection();
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		
		if (conn.getResponseCode()!=200) {
			out.println("图片下载失败");
			return;
		}
		
		/*File file = new File(path);
		if (!file.exists()) {
			return;
		}*/
		System.out.println(req.getServletContext().getRealPath("0"));
		//linux文件路径  /home/0
		//IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get("/home", "0")));
		IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get(req.getServletContext().getRealPath("0"))));
        ImageSearcher searcher;
        switch(method) {
        	case "FCTH":
        		searcher = new GenericFastImageSearcher(30, FCTH.class);
        		break;
        	case "AutoColorCorrelogram":
        		searcher = new GenericFastImageSearcher(30, AutoColorCorrelogram.class);
        		break;
        	case "CEDD":
        		searcher = new GenericFastImageSearcher(30, CEDD.class);
        		break;
        	default:
        		searcher = new GenericFastImageSearcher(30, FCTH.class);
        		break;
        }
//        ImageSearcher searcher = new GenericFastImageSearcher(30, AutoColorCorrelogram.class);

        // searching with a image file ...
        ImageSearchHits hits = searcher.search(ImageIO.read(conn.getInputStream()), ir);
        for (int i = 0; i < hits.length(); i++) {
            String goodsId = ir.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
            out.println("<img width=\"200px\" src=\""+goodsId.replace("D:\\search_res\\img\\", "http://192.168.1.190:8080/ImageService/img/") +"\" \\>");
            System.out.println(hits.score(i) + ": \t" + goodsId);
        }
        out.println("</body>");
		out.println("</html>");
	}
	
	

}
