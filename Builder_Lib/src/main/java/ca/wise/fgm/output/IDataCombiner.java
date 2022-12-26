package ca.wise.fgm.output;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import ca.wise.config.proto.ServerConfiguration;
import ca.wise.api.WISE;

public interface IDataCombiner {

	IDataWriter combine(String jobName);
	
	void initialize(WISE input, ServerConfiguration config, String jobDir);

	default String extension(String path) {
		return com.google.common.io.Files.getFileExtension(path);
	}

	default String filename(String path) {
		return com.google.common.io.Files.getNameWithoutExtension(path);
	}

	default String readyFile(String outPath, String filename) {
		if (filename.length() < 1) {
			return filename;
		}
		//filename = filename.replace("/mnt/prometheus/6.2.1.21/", "C:/gitprojects/debug/wiseserver/");
		File in = new File(filename);
		String ext = extension(filename);
		String name = filename(filename);
		String newName = name + "." + ext;
		String fullpath = outPath + "/Inputs/" + newName;
		File out = new File(fullpath);
		int i = 2;
		while (out.exists()) {
			newName = name + i + "." + ext;
			fullpath = outPath + "/Inputs/" + newName;
			out = new File(fullpath);
			i++;
		}
		try {
			String inPath = in.getAbsolutePath();
			Files.copy(Paths.get(inPath), Paths.get(out.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
			if (inPath.endsWith(".shp")) {
				String newNewName;
				inPath = inPath.substring(0, inPath.length() - 3) + "shx";
				Path shx = Paths.get(inPath);
				inPath = inPath.substring(0, inPath.length() - 3) + "dbf";
				Path dbf = Paths.get(inPath);
				if (Files.exists(shx)) {
					if (i > 2)
						newNewName = name + i + ".shx";
					else
						newNewName = name + ".shx";
					out = new File(outPath + "/Inputs/" + newNewName);
					Files.copy(shx, Paths.get(out.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
				}
				if (Files.exists(dbf)) {
					if (i > 2)
						newNewName = name + i + ".dbf";
					else
						newNewName = name + ".dbf";
					out = new File(outPath + "/Inputs/" + newNewName);
					Files.copy(dbf, Paths.get(out.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (FileNotFoundException|FileAlreadyExistsException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "Inputs/" + newName;
	}

	default String readyOutputFile(String input) {
		int index = input.indexOf("..");
		while (index >= 0) {
			input = input.substring(index + 2);
			index = input.indexOf("..");
		}
		if (input.startsWith("/")) {
			input = input.substring(1);
		}
		return "Outputs/" + input;
	}
	
	public static IDataCombiner create(Message startMessage, Object options) {
		if (startMessage == Message.STARTJOB)
			return new DataCombiner(options);
		else if (startMessage == Message.STARTJOB_PB)
			return new PBCombiner(options);
		else if (startMessage == Message.STARTJOB_PB_V2)
			return new PBCombinerV2(options);
		return null;
	}
}
