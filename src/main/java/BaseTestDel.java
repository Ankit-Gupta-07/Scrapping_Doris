import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class BaseTestDel {
    public static Properties locators;
    public static Properties config;
    public static WebDriver driver;
    public static String TotalPages;
    public static int count;
    public static boolean last_isFIle_empty;
    public static int totalData;
    public static String SRO_name;
    public static String loc_name;
    public static int local_int;
    public static int hh;
    public static int mm;
    public static boolean flag_locality_ast = false;
    public static List<String> allLocalityIndexes;
    public static String moreIfApplicable = "";
    public static boolean previousPageHavingMoreButton = false;
    public static int moreCount = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        int newLocator_index = 0;
        int maxLocatorAttempt = 50;
        while (newLocator_index < maxLocatorAttempt) {
            int maxAttempts = 5;
            int attempt = 0;
            boolean success = false;
            while (attempt < maxAttempts && !success) {
                attempt++;
                System.out.println("Attempt " + attempt);
                try {
                    totalData = 0;
                    count = 0;
                    cal();
                    basePage();

                    if (flag_locality_ast) {
                        previousYearLoopWithAstrik();
                        System.out.println("Total Data fetched.: " + totalData);
                        System.out.println("Total Previous buttons found.: " + count);
                    } else {
                        previousYearLoopWithoutAstrik();
                        System.out.println("Total Data fetched archival data: " + totalData);
                        System.out.println("Total Previous buttons found archival data: " + count);
                    }

                    // Mark success
                    success = true;

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed attempt " + attempt);
                    if (driver != null) {
                        driver.quit();
                    }
                    if (attempt < maxAttempts) {
                        System.out.println("Retrying...");
                        Thread.sleep(2000); // Optional: Sleep for 2 seconds before retrying
                    }
                }
            }

            if (!success) {
                System.out.println("Reached maximum number of attempts and failed process for." + loc_name);
            }
            if (driver != null) {
                driver.quit();
            }
            local_int = local_int + 3;
            writeConfigFile();
            System.out.println("local_int value incremented by 3 and the value is : " + local_int);
            newLocator_index++;

        }
    }

    public static void writeConfigFile() throws IOException {
        config.setProperty("localInt", String.valueOf(local_int));
        try (FileOutputStream fileOut = new FileOutputStream("config.properties")) {
            config.store(fileOut, "Updated Locality_index");
        }
        System.out.println("Updated Locality_index");
    }


    public static void cal() throws InterruptedException {
        Calendar calendar = Calendar.getInstance();
        Thread.sleep(200);
        hh = calendar.get(Calendar.HOUR_OF_DAY);
        mm = calendar.get(Calendar.MINUTE);
    }

    public static void basePage() throws IOException, InterruptedException {
        BufferedReader rdr = new BufferedReader(new FileReader("locators.properties"));
        locators = new Properties();
        locators.load(rdr);
        BufferedReader con = new BufferedReader(new FileReader("config.properties"));
        config = new Properties();
        config.load(con);

//initialize WebDriver
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get("https://esearch.delhigovt.nic.in/Complete_search_without_regyear.aspx");

        SRO_name = config.getProperty("SROname");
        local_int = Integer.parseInt(config.getProperty("localInt"));
        System.out.println("Index, local_int value: " + local_int);
//        local_int = value is to be given at psvm method to prevent reinitializing the value again to this value
        Select obj = new Select(driver.findElement(By.xpath(locators.getProperty("SRO"))));
        obj.selectByVisibleText(SRO_name);
        Thread.sleep(2000);
        List<WebElement> tempAllIndex = driver.findElements(By.xpath(locators.getProperty("allLocalityIndexesLOCATOR")));
        allLocalityIndexes = Arrays.asList(new String[tempAllIndex.size()]);
        Thread.sleep(200);
        for (int i = 0; i < tempAllIndex.size(); i++) {
            String s = tempAllIndex.get(i).getAttribute("Value").trim();
            allLocalityIndexes.set(i, s);
        }
        System.out.println("Total locality present at " + SRO_name + " are: " + allLocalityIndexes.size());
//        for (int j = 0; j < allLocalityIndexes.size(); j++) {
//            System.out.print(allLocalityIndexes.get(j) + " , ");
//        }

        Select obj1 = new Select(driver.findElement(By.xpath(locators.getProperty("Locality"))));
        obj1.selectByIndex(local_int);
        // for checking pink localities flag
        WebElement selectedOption = obj1.getFirstSelectedOption();
        loc_name = selectedOption.getText();
        System.out.println("Locality selected: " + loc_name);
        if (loc_name.contains("*")) {
            flag_locality_ast = true;
        }
        WebElement search = driver.findElement(By.xpath(locators.getProperty("search")));
        Thread.sleep(500);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView();", search);

        extractAndPushCaptcha(driver);//Reading and writing captcha

        Thread.sleep(3000);
        search.click();
        Thread.sleep(500);
    }

    public static void extractAndPushCaptcha(WebDriver driver) throws InterruptedException, IOException {
        Thread.sleep(1000);

        // Locate the image element and get the URL of the captcha image
        WebElement imageElement = driver.findElement(By.xpath(".//div[@class='btn btn-sm']/img"));
        String imageEndPoint = imageElement.getAttribute("src");
//        System.out.println("imageEndPoint - " + imageEndPoint);

        // Convert image to Base64
        String base64Image = encodeImageToBase64(imageEndPoint);

//        System.out.println("Base64Image - " + base64Image);

        // Call the API to get captcha text
        String captchaText = solveCaptcha(base64Image);

        // Send captcha text to the input field
        driver.findElement(By.xpath(locators.getProperty("captchsTextPlace"))).sendKeys(captchaText);
    }

    private static String encodeImageToBase64(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        InputStream is = url.openStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }

        is.close();
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Ensure the Base64 string is in ASCII format
        return new String(base64Image.getBytes(), "US-ASCII");
    }

    private static String solveCaptcha(String base64Image) throws IOException, InterruptedException {
        String apiUrl = "https://api.apitruecaptcha.org/one/gettext";
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Thread.sleep(500);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        // JSON request payload
        String jsonInputString = new JSONObject()
                .put("userid", "shubham97shbh@gmail.com")
                .put("apikey", "oVEGbwR4M23tElIfY1D9")
                .put("data", base64Image)
                .toString();

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Get response
        Thread.sleep(100);
        StringBuilder response;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            Thread.sleep(1000);
            response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        // Parse the JSON response and get the 'result' field
        String jsonResponse = response.toString();
        System.out.println("Output response:" + jsonResponse);
        JSONObject jsonObject = new JSONObject(jsonResponse);
        return jsonObject.getString("result");
    }

    public static void rowExtend() throws InterruptedException {
        Thread.sleep(500);
        WebElement rowExtend = driver.findElement(By.xpath(locators.getProperty("rowExtend")));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView();", rowExtend);
        Thread.sleep(1000);
        rowExtend.click();
        Thread.sleep(1000);
        Select rowEx = new Select(rowExtend);
        rowEx.selectByValue("20");
        Thread.sleep(2500);
        driver.navigate().refresh();
        Thread.sleep(1000);
    }


    public static void flagTypePage() throws InterruptedException {
        WebElement table = driver.findElement(By.xpath(".//table[@id='ctl00_ContentPlaceHolder1_gv_search']"));
        // Find all rows within the table
        Thread.sleep(1500);
        List<WebElement> rows = table.findElements(By.xpath("tbody/tr"));
        for (int i = rows.size() - 2; i < rows.size(); i++) {
            WebElement row = rows.get(i);
            List<WebElement> last_cell_values = row.findElements(By.xpath("td"));

//          only for flag
            for (WebElement cell : last_cell_values) {
                String cellText = cell.getText().trim();
                if (cellText.contains("Show rows:")) {
                    last_isFIle_empty = true;
                } else {
                    last_isFIle_empty = false;
                }
            }
        }
    }

    public static void fetchData() throws IOException, InterruptedException {

        int x;
        WebElement regDate = driver.findElement(By.xpath(locators.getProperty("yearText")));
        String regYear = regDate.getText().trim().substring(regDate.getText().trim().length() - 4);
        System.out.println("Reg Year: " + regYear);
        if ((!loc_name.contains("*")) && (Integer.parseInt(regYear) < 1995)) {
            System.out.println("Reg Year found: " + regYear + " which is less than 1995");
            driver.quit();
        }
        WebElement table = driver.findElement(By.xpath(".//table[@id='ctl00_ContentPlaceHolder1_gv_search']"));
        // Find all rows within the table
        List<WebElement> rows = table.findElements(By.xpath("tbody/tr"));
        // FileWriter for CSV output
        try (FileWriter csvWriter = new FileWriter(SRO_name + "_" + local_int + "_" + hh + mm + "hrs" +
                ".csv", true)) {
            // Check if file is empty to determine if we need to write the header
            flagTypePage();
            boolean isFileEmpty = new File(SRO_name + "_" + local_int + "_" + hh + mm + "hrs" + ".csv").length() == 0;
            // Iterate over each row, skipping the last two rows
            if (last_isFIle_empty) {

                x = 2;
            } else {
                x = 1;
            }
//            rowExtend(); will use later

            int tempRowCount = rows.size() - x - 1;
            totalData = totalData + tempRowCount;
            System.out.println("Data fetched till now: " + totalData);
            // Iterate over each row, skipping the last two rows / one row
            for (int i = 0; i < rows.size() - x; i++) {
                WebElement row = rows.get(i);
                List<WebElement> cells = row.findElements(By.xpath("td"));
                StringBuilder csvLine = new StringBuilder();

                // Iterate over each cell in the row
                for (WebElement cell : cells) {
                    String cellText = getString(cell);
                    csvLine.append(cellText).append(",");
                }
                if (csvLine.length() > 0) {
                    csvLine.append(loc_name).append(",");
                    csvLine.append(SRO_name).append(",");
                }
                // Write the CSV line to file
                csvWriter.append(csvLine.toString().replaceAll(",$", "")).append("\n");
            }
            // Flush and close the CSV writer
            csvWriter.flush();
            csvWriter.close();
//            System.out.println("CSV file has been written successfully.");
            Thread.sleep(1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getString(WebElement cell) {
        String cellText = cell.getText().trim();
        if (cellText.contains(loc_name)) {
            // Split the scraped text into lines
//                        cellText = cellText.replace("\n", " ");
            cellText = ", " + cellText;
        }
        // Handle cases where the cell text contains commas
        if (cellText.contains(",")) {
            cellText = "\"" + cellText + "\"";
        }
        return cellText;
    }

    // for pink localities
    public static void sheetLoopWithAstrik() throws InterruptedException {
        // Scroll table page
        Thread.sleep(3000);
        // Page reload as it's not showing more
        driver.navigate().refresh();
        Thread.sleep(1000);
        WebElement table = driver.findElement(By.xpath(".//table[@id='ctl00_ContentPlaceHolder1_gv']"));

        for (int i = 0; i < 3; i++) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView();", table);
            Thread.sleep(1000);
        }
        // Find all rows within the table
        List<WebElement> rows = table.findElements(By.xpath("tbody/tr"));
        // FileWriter for CSV output
        loc_name = loc_name.replace("*", "").trim();
        try (FileWriter csvWriter = new FileWriter(SRO_name + "_" + local_int + "_" + hh + mm + "hrs" +
                ".csv", true)) {
            Thread.sleep(1000);
            // Check if file is empty to determine if we need to write the header
            boolean isFileEmpty = new File(SRO_name + "_" + local_int + "_" + hh + mm + "hrs" + ".csv").length() == 0;
            // Iterate over each row, skipping the last two rows
//            rowExtend(); will use later
            int tempRowCount = rows.size() - 2;
            totalData = totalData + tempRowCount;
            System.out.println("Data fetched till now: " + totalData);
            // Iterate over each row, skipping the last two rows / one row
            for (int i = 0; i < rows.size() - 1; i++) {
                WebElement row = rows.get(i);
                List<WebElement> cells = row.findElements(By.xpath("td"));
                StringBuilder csvLine = new StringBuilder();

                // Iterate over each cell in the row
                for (WebElement cell : cells) {
                    String cellText = cell.getText().trim();
                    // Handle cases where the cell text contains commas
                    if (cellText.contains(",")) {
                        cellText = "\"" + cellText + "\"";
                    }
                    csvLine.append(cellText).append(",");
                }
                if (csvLine.length() > 0) {
                    csvLine.append(loc_name).append(",");
                    csvLine.append(SRO_name).append(",");
                }
                // Write the CSV line to file
                csvWriter.append(csvLine.toString().replaceAll(",$", "")).append("\n");
            }
            // Flush and close the CSV writer
            csvWriter.flush();
            csvWriter.close();
//            System.out.println("CSV file has been written successfully.");
            Thread.sleep(1000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void sheetLoop() {
        try {
            moreButtonOnWithoutAstrikPages();
//        GET NO OF SHEET
            Thread.sleep(1000);
            // Initial fetch

            if (last_isFIle_empty && !previousPageHavingMoreButton) {
                rowExtend();
            }
            fetchData();
            System.out.println("User fetches all data from page: 1");

            if (last_isFIle_empty) {
                TotalPages = driver.findElement(By.xpath(locators.getProperty("TotalPages"))).getText().trim();
                System.out.println("total sheets: " + TotalPages);
                // Loop to click "Previous Year" button and fetch data again
                for (int i = 2; i <= Integer.parseInt(TotalPages); i++) {
                    System.out.println("User fetches all data from page: " + i);
                    WebElement nextSheet = driver.findElement(By.xpath(locators.getProperty("nextSheet")));
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView();", nextSheet);
                    Thread.sleep(2000);
                    nextSheet.click();
                    Thread.sleep(3000);
                    fetchData();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void previousYearLoopWithAstrik() {
        while (true) {
            try {
                Thread.sleep(1000);
                sheetLoopWithAstrik();
                WebElement more = driver.findElement(By.xpath(locators.getProperty("moreButton")));
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView();", more);
                Thread.sleep(3000);
                more.click();
                count++;
                System.out.println("Previous year click counter: " + count);
                Thread.sleep(3000);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Last previous button found. Stopping..");
                break;
            }
        }
    }

    public static void previousYearLoopWithoutAstrik() throws InterruptedException {
        while (true) {
            moreButtonOnWithoutAstrikPages();
            try {
                // Initial fetch
                last_isFIle_empty = false;
                sheetLoop();
                // Loop to click "Previous Year" button and fetch data again
                Thread.sleep(1000);
                if (last_isFIle_empty) {
                    WebElement showRowTextInput = driver.findElement(By.xpath(locators.getProperty("showRowTextInput")));
                    Thread.sleep(1000);
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView();", showRowTextInput);
                    Thread.sleep(1000);
                    showRowTextInput.click();
                    Thread.sleep(500);
                    showRowTextInput.clear();
                    Thread.sleep(1000);
                    System.out.println("Clear show row text=======================");
                    WebElement temp = driver.findElement(By.xpath(locators.getProperty("showRowTextInput")));

                    temp.sendKeys(Keys.ENTER);
                    Thread.sleep(3000);
                    WebElement temp1 = driver.findElement(By.xpath(locators.getProperty("showRowTextInput")));
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView();", temp1);
                    Thread.sleep(1000);
                }


                WebElement previousYear = driver.findElement(By.xpath(locators.getProperty("previousYear")));
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView();", previousYear);
                Thread.sleep(3000);
                previousYear.click();
                count++;
                System.out.println("Previous year click counter: " + count);
                Thread.sleep(3000);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Last previous button found. Stopping..");
                break;
            }
        }
    }

    public static void moreButtonOnWithoutAstrikPages() throws InterruptedException {
        Thread.sleep(500);
        List<WebElement> moreOrPreviousAll = driver.findElements(By.xpath(locators.getProperty("moreOrPrevious")));
        for (WebElement ele : moreOrPreviousAll) {
            String temp = ele.getAttribute("Value").trim().toLowerCase();
//            String temp = ele.getText().trim().toLowerCase();
            System.out.println("END button available on screen: " + temp);
            if (temp.contains("more")) {
                previousPageHavingMoreButton = true;
                moreCount++;
                break;
            } else if (temp.contains("previous")) {
                break;
            }
        }
        if (previousPageHavingMoreButton && moreCount == 1) {
            moreCount++;
            System.out.println("Found More button in \nSRON-" + SRO_name + "\nLocality-" + loc_name);
            while (true) {
                WebElement table = driver.findElement(By.xpath(locators.getProperty("moreTableLoc")));
                List<WebElement> rows = table.findElements(By.xpath("tbody/tr"));
                try (FileWriter csvWriter = new FileWriter(SRO_name + "_" + local_int + "_" + hh + mm + "hrs" + "_with more" +
                        ".csv", true)) {
                    Thread.sleep(1000);
                    boolean isFileEmpty = new File(SRO_name + "_" + local_int + "_" + hh + mm + "hrs" + ".csv").length() == 0;
                    int tempRowCount = rows.size() - 2;
                    totalData = totalData + tempRowCount;
                    System.out.println("Data fetched till now: " + totalData);
                    for (int i = 0; i < rows.size() - 1; i++) {
                        WebElement row = rows.get(i);
                        List<WebElement> cells = row.findElements(By.xpath("td"));
                        StringBuilder csvLine = new StringBuilder();
                        for (WebElement cell : cells) {
                            String cellText = cell.getText().trim();
                            if (cellText.contains(",")) {
                                cellText = "\"" + cellText + "\"";
                            }
                            csvLine.append(cellText).append(",");
                        }
                        if (csvLine.length() > 0) {
                            csvLine.append(loc_name).append(",");
                            csvLine.append(SRO_name).append(",");
                        }
                        csvWriter.append(csvLine.toString().replaceAll(",$", "")).append("\n");
                    }
                    csvWriter.flush();
                    csvWriter.close();
                    Thread.sleep(1000);
                    WebElement more = driver.findElement(By.xpath(locators.getProperty("moreButton")));
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView();", more);
                    Thread.sleep(3000);
                    more.click();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}
