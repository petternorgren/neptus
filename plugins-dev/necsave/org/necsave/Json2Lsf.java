package org.necsave;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.eclipsesource.json.JsonObject;

import info.necsave.plot.LogParser;
import info.necsave.plot.LogProcessor;
import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.lsf.LsfMessageLogger;

public class Json2Lsf implements LogProcessor {

	protected LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
	protected LinkedHashMap<String, Method> methods = new LinkedHashMap<>();
	{
		for (Method m : getClass().getMethods()) {
			if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == JsonObject.class) {
				methods.put(m.getName(), m);
			}
		}
	}
	
	@Override
	public String getName() {
		return "JSON 2 LSF";
	}

	public void process(JsonObject obj) {
		String msgName = obj.get("abbrev").asString();
		if (msgName.equals("PlatformInfo")) {
			if (!isSourceKnown(obj)) {
				Announce announce = new Announce();
				announce.setSrc(getPlatformSrc(obj));
				announce.setSysName(obj.get("platform_name").asString());
				announce.setTimestamp(getTimestamp(obj)/1000.0);
				LsfMessageLogger.log(announce);
			}
			platformNames.put(getPlatformSrc(obj), obj.get("platform_name").asString());
		}
		
		
		if (methods.containsKey(msgName)) {
			try {
				methods.get(msgName).invoke(this, obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected long getTimestamp(JsonObject msg) {
		return Long.parseLong(msg.get("timestamp").asString()) / 1000;
		
	}
	
	protected String getName(int id) {
		if (!platformNames.containsKey(id))
			return "Platform "+id;
		
		return platformNames.get(id);
	}

	protected int getPlatformSrc(JsonObject msg) {
		return Integer.parseInt(msg.get("platform_src").asString());
	}
	
	protected boolean isSourceKnown(JsonObject msg) {
		return platformNames.containsKey(getPlatformSrc(msg));
	}
	
	protected String getPlatformName(JsonObject msg) {
		return getName(getPlatformSrc(msg));
	}

	protected int getModuleSrc(JsonObject msg) {
		return Integer.parseInt(msg.get("module_src").asString());
	}
	
	@Override
	public void logFinished() {
		LsfMessageLogger.close();
	}
	
	public void Kinematics(JsonObject m) {
		if (!isSourceKnown(m))
			return;
		
		if (getPlatformSrc(m) == 14)
			return;
		
		EstimatedState state = new EstimatedState();
		JsonObject coords = m.get("waypoint").asObject();
		double lat = Double.parseDouble(coords.get("latitude").asString());
		double lon = Double.parseDouble(coords.get("longitude").asString());
		double depth = Double.parseDouble(coords.get("depth").asString());
		double alt = Double.parseDouble(coords.get("altitude").asString());
		double yaw = Double.parseDouble(m.get("heading").asString());
		double u = Double.parseDouble(m.get("speed").asString());
		state.setSrc(getPlatformSrc(m));
		state.setLat(lat);
		state.setLon(lon);
		state.setDepth(depth);
		state.setAlt(alt);
		state.setPsi(yaw);
		state.setU(u);
		state.setTimestamp(getTimestamp(m)/1000.0);
		LsfMessageLogger.log(state);
		
	}
	
	private static File[] collectLogs(File topDir) {
		HashSet<File> found = new HashSet<>();
		collectLogs(topDir, found);
		return found.toArray(new File[0]);
	}
	
	private static void collectLogs(File topDir, HashSet<File> found) {
		for (File f : topDir.listFiles()) {
			if (f.isDirectory())
				collectLogs(f, found);
			else if (f.getName().endsWith(".json"))
				found.add(f);			
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select logs folder");
		chooser.setAcceptAllFileFilterUsed(false);
		if (!(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION))
			return;
		
		File topDir = chooser.getSelectedFile();
		LsfMessageLogger.changeLogBaseDir(topDir.getAbsolutePath()+"/");
		File[] files = collectLogs(topDir);
		LogParser parser = new LogParser(files);
		parser.process(false, new Json2Lsf());
		JOptionPane.showMessageDialog(null, files.length+" files were merged to "+LsfMessageLogger.getLogDirSingleton());
	}

}
