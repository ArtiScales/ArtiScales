package fr.ign.cogit.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONObject;

import fr.ign.parameters.Parameter;
import fr.ign.parameters.ParameterComponent;
import fr.ign.parameters.Parameters;

public class TransformXMLToJSON {
  public static void convert(File folderIn) throws Exception {
    for (File fileIn : folderIn.listFiles()) {
      System.out.println(fileIn);
      if (fileIn.isDirectory()) {
        convert(fileIn);
      } else {
        if (fileIn.getName().endsWith(".xml")) {
          Parameters p = Parameters.unmarshall(fileIn);
          JSONObject obj = new JSONObject();
          for (ParameterComponent c : p.entry) {
            Parameter param = (Parameter) c;
            obj.put(param.getKey(), param.getValue());
          }
          try (FileWriter file = new FileWriter(new File(folderIn, fileIn.getName().replaceAll(".xml", ".json")))) {
            file.write(obj.toJSONString());
            file.flush();
          } catch (IOException e) {
            e.printStackTrace();
          }
//          System.out.print(obj);
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    convert(new File("/home/mcolomb/workspace/ArtiScales/fakeWorld/paramFolder/"));
  }
}
