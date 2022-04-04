package br.com.wpivotto.filebox.api;

import static org.restexpress.Flags.Auth.PUBLIC_ROUTE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;

import org.imgscalr.Scalr;
import org.restexpress.Request;
import org.restexpress.Response;
import org.restexpress.RestExpress;
import org.restexpress.pipeline.SimpleConsoleLogMessageObserver;

import com.strategicgains.restexpress.plugin.cors.CorsHeaderPlugin;

import br.com.wpivotto.filebox.index.Configs;
import br.com.wpivotto.filebox.index.DocSearcher;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

@ApplicationScoped
public class ApiServer {
	
	private final Logger logger = Logger.getLogger(ApiServer.class.getName());
	
	private static final String SERVICE_NAME = "api";
	private static final String BASE_URL = "filebox";
	private Integer port = 9999;

	private RestExpress server;
	private Configs configs;
	private DocSearcher searcher;
 
	@Inject
	public ApiServer(Configs configs, DocSearcher searcher) {
		this.configs = configs;
		this.searcher = searcher;
		setup();
	}

	public void start() {
		try {
			server.bind(port);
			logger.log(Level.INFO, "API Server running at port " + port);
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.INFO, "Failed to start API Server at port " + port);
		}
	}

	public void stop() {
		try {
			
			if (server != null) {
				server.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setup() {
		
		if (server == null) {
		
			server = new RestExpress()
				.setName(SERVICE_NAME)
				.setBaseUrl(BASE_URL)
				.addMessageObserver(new SimpleConsoleLogMessageObserver());
		
			port = configs.getHttpPort();
		}

		server.uri("/search", this).action("searchTerm", HttpMethod.GET);
		server.uri("/img/{filename}", this).action("serveImage", HttpMethod.GET);
		
		new CorsHeaderPlugin("*").flag(PUBLIC_ROUTE).exposeHeaders("Location", "Vary", "Content-disposition").register(server);
		
	}
	
	public void searchTerm(Request request, Response response) {
		
		String term = request.getHeader("q", "Error: term expected");
		
		byte[] bytes = searcher.searchFor(term);

		response.setBody(Unpooled.wrappedBuffer(bytes));
		response.noSerialization(); 
		response.setResponseStatus(HttpResponseStatus.OK);
		response.setContentType("application/pdf");
		response.addHeader("Content-disposition", "attachment; filename=results.pdf");
		response.addHeader("Access-Control-Expose-Headers", "Content-disposition");
		
	}
	
	public void serveImage(Request request, Response response) throws Exception {
		
		String filename = request.getHeader("filename", "Error: filename expected");
		Integer targetWidth = getIntParameter(request, "w");
		Integer targetHeight = getIntParameter(request, "h");
		
		Path path = configs.getImageLocation(filename);
		
		String contentType = Files.probeContentType(path);
		byte[] bytes = scaleImage(path, targetWidth, targetHeight, contentType);
		response.setBody(Unpooled.wrappedBuffer(bytes));
		response.noSerialization(); 
		response.setResponseStatus(HttpResponseStatus.OK);
		response.setContentType(contentType);
		response.addHeader("Content-disposition", "inline");
		response.addHeader("Access-Control-Expose-Headers", "Content-disposition");
		
	}
	
	private Integer getIntParameter(Request request, String name) {
		try {
			String param = request.getQueryStringMap().get(name);
			return Integer.parseInt(param);
		} catch (Exception e) {
			return null;
		}
	}
	
	private byte[] scaleImage(Path path, Integer targetWidth, Integer targetHeight, String contentType) throws IOException {

		BufferedImage img = ImageIO.read(path.toFile());
		
		int w = targetWidth != null ? targetWidth : img.getWidth();
		int h = targetHeight != null ? targetHeight : img.getHeight();
		
		BufferedImage scaledImg = Scalr.resize(img, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, w, h, Scalr.OP_ANTIALIAS);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String format = contentType.substring(contentType.indexOf("/") + 1); 
		ImageIO.write(scaledImg, format, baos);
		return baos.toByteArray();

	}
 
}
