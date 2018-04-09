package fr.ign.cogit.util;

import java.io.File;

public class ExtensionLowerCase {

	public static void main(String[] args) {
		File folder = new File("/home/mcolomb/donnee/autom/besac2/dataIn/bati");
	}

	public static File lowerCaseExtention(File folder) {
		for (File fSub : folder.listFiles()) {

			if (fSub.isDirectory()) {
				continue;
			}

			String path = fSub.getAbsolutePath();
			String[] filePart = path.split("\\.");

			System.out.println(filePart.length);

			if (filePart.length != 2) {
				System.out.println("2 parts : " + fSub);
			}

			String newName = filePart[0];
			String extension = filePart[1].toLowerCase();

			File newNameFile = new File(newName + "." + extension);

			fSub.renameTo(newNameFile);
		}
		return folder;
	}
}
