/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ro.cornholio.vision.text;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Status;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;


/**
 * A sample application that uses the Vision API to OCR text in an image.
 */
@SuppressWarnings("serial")
@Controller
@EnableAutoConfiguration
public class TextApp {
  private static final int MAX_RESULTS = 1;
  private static final int BATCH_SIZE = 10;

  /**
   * Be sure to specify the name of your application. If the application name is {@code null} or
   * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
   */
  private static final String APPLICATION_NAME = "VISION";
  public static final String ROOT = "upload-dir";

  /**
   * Connects to the Vision API using Application Default Credentials.
   */
  public static Vision getVisionService() throws IOException, GeneralSecurityException {
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(VisionScopes.all());
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    return new Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
  }

  @RequestMapping("/")
  String home() {
    return "uploadForm";
  }

  @RequestMapping(method = RequestMethod.POST, value = "/")
  public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
    if (!file.isEmpty()) {
      try {
        byte[] bytes = IOUtils.toByteArray(file.getInputStream());
        List<ImageText> texts = detectText(bytes);
        for(ImageText txt: texts) {
          for(EntityAnnotation annotation : txt.textAnnotations()) {
            System.out.println(annotation.getDescription());
          }
        }

        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded !");
      } catch (IOException|RuntimeException e) {
        redirectAttributes.addFlashAttribute("message", "Failed to upload => " + e.getMessage());
      }
    } else {
      redirectAttributes.addFlashAttribute("message", "Failed to upload because it was empty");
    }

    return "redirect:/";
  }

  @RequestMapping(method = RequestMethod.POST, value = "/ajax")
  @ResponseBody
  public String handleFileUpload(@RequestParam("imgBase64") String file,
                                 RedirectAttributes redirectAttributes) {
    System.out.println(file.length());
    if (!file.isEmpty()) {
      try {
        byte[] bytes = Base64.decodeBase64(file.replaceAll("data:image/.+;base64,",""));
        List<ImageText> texts = detectText(bytes);
        System.out.println(texts.size());

        for(ImageText txt: texts) {
          System.out.println(txt.textAnnotations());

          for(EntityAnnotation annotation : txt.textAnnotations()) {
            System.out.println(annotation.getDescription());
          }
        }

        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded !");
        return texts.get(0).textAnnotations().toString();
      } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute("message", "Failed to upload => " + e.getMessage());
      }
    } else {
      redirectAttributes.addFlashAttribute("message", "Failed to upload because it was empty");
    }

    return "[]";
  }

  /**
   * Annotates an image using the Vision API.
   */
  public static void main(String[] args) throws IOException, GeneralSecurityException {
    if (args.length > 1) {
      System.err.println("Usage:");
      System.err.printf(
          "\tjava %s inputDirectory\n",
          TextApp.class.getCanonicalName());
      System.exit(1);
    }
    SpringApplication.run(TextApp.class, args);


  }

  private final Vision vision;

  /**
   * Constructs a {@code TextApp} using the {@link Vision} service.
   */
  public TextApp(Vision vision) {
    this.vision = vision;
  }

  public TextApp() throws IOException, GeneralSecurityException {
    this.vision = getVisionService();
  }



  /**
   * Gets up to {@code maxResults} text annotations for images stored at {@code paths}.
   */
  public ImmutableList<ImageText> detectText(byte[] data) {
    ImmutableList.Builder<AnnotateImageRequest> requests = ImmutableList.builder();
    try {
        requests.add(
            new AnnotateImageRequest()
                .setImage(new Image().encodeContent(data))
                .setFeatures(ImmutableList.of(
                    new Feature()
                        .setType("TEXT_DETECTION")
                        .setMaxResults(MAX_RESULTS))));


      Vision.Images.Annotate annotate =
          vision.images()
              .annotate(new BatchAnnotateImagesRequest().setRequests(requests.build()));
      // Due to a bug: requests to Vision API containing large images fail when GZipped.
      annotate.setDisableGZipContent(true);
      BatchAnnotateImagesResponse batchResponse = annotate.execute();

      ImmutableList.Builder<ImageText> output = ImmutableList.builder();
        AnnotateImageResponse response = batchResponse.getResponses().get(0);
        output.add(
            ImageText.builder()
                .textAnnotations(
                    MoreObjects.firstNonNull(
                        response.getTextAnnotations(),
                        ImmutableList.<EntityAnnotation>of()))
                .error(response.getError())
                .build());

      return output.build();

    } catch (IOException ex) {
      System.out.println(ex);

      // Got an exception, which means the whole batch had an error.
      ImmutableList.Builder<ImageText> output = ImmutableList.builder();
        output.add(
            ImageText.builder()
                .textAnnotations(ImmutableList.<EntityAnnotation>of())
                .error(new Status().setMessage(ex.getMessage()))
                .build());
      return output.build();
    }
  }

}
