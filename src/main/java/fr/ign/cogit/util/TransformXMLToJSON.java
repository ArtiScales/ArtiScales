package fr.ign.cogit.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONObject;

import fr.ign.parameters.Parameter;
import fr.ign.parameters.ParameterComponent;
import fr.ign.parameters.Parameters;

public class TransformXMLToJSON {
  public static void convert(File folderIn, File folderOut) throws Exception {
    for (File fileIn : folderIn.listFiles()) {
      System.out.println(fileIn);
      if (fileIn.isDirectory()) {
        File fileOut = new File(folderOut, fileIn.getName());
        convert(fileIn, fileOut);
      } else {
        if (fileIn.getName().endsWith(".xml")) {
          Parameters p = Parameters.unmarshall(fileIn);
          JSONObject obj = new JSONObject();
          for (ParameterComponent c : p.entry) {
            Parameter param = (Parameter) c;
            obj.put(param.getKey(), param.getValue());
          }
          folderOut.mkdirs();
          try (FileWriter file = new FileWriter(new File(folderOut, fileIn.getName().replaceAll(".xml", ".json")))) {
            file.write(obj.toJSONString());
            file.flush();
          } catch (IOException e) {
            e.printStackTrace();
          }
          System.out.print(obj);
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    String folderName = TransformXMLToJSON.class.getClassLoader().getResource(".").getPath();
    File folderOut = new File("JSON");
    folderOut.mkdirs();
    File folder = new File(folderName);
    convert(folder, folderOut);
  }
}
