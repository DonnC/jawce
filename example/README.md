# JAWCE Example ChatBots
Example chatbot

### **Chatbot Flow Configuration**

1. **START_MENU**  
   **Type:** `button`  
   **Template:** `zw.co.dcl.jawce.chatbot.hooks.GreetingHook:getDefaultUsername`  
   **Message:**
    - **Title:** PickDrive
    - **Body:**
      ```
      Hi {{ user }}, I'm PickDrive Assistant - making your inner-city travelling a breeze.  
      To start, click the button below.  
      ```  
    - **Buttons:** [Proceed]  
      **Routes:**
    - `proceed → PICKUP_DRIVE_MAIN_MENU`

---

2. **PICKUP_DRIVE_MAIN_MENU**  
   **Type:** `button`  
   **Prop:** `RideType`  
   **Message:**
    - **Title:** PickDrive
    - **Body:** To offer you an exclusive riding experience, select your preferred ride type
    - **Buttons:** [Ride, Standard, Luxury]  
      **Routes:**
    - `re:.* → PICKUP_LOCATION`

---

3. **PICKUP_LOCATION**  
   **Type:** `request-location`  
   **Message:**
    - Where would you like to be picked from?  
      **Routes:**
    - `location_request → DESTINATION_LOCATION`

---

4. **DESTINATION_LOCATION**  
   **Type:** `request-location`  
   **Message:**
    - Where would you like to go?  
      **Routes:**
    - `location_request → RIDE_OFFER`

---
5. **RIDE_OFFER**  
   **Type:** `button`  
   **Message:**
    - **Title:** Ride Fee
    - **Body:**
      ```
      Your ride fee to your destination is USD $3.50  
      You will arrive in approx ~ 8 mins.  
      ```  
    - **Buttons:** [Accept, Counter Offer]  
      **Routes:**
    - `accept → RIDE_COMMENT`
    - `counter_offer → RIDE_COUNTER_OFFER`

---
6. **RIDE_COMMENT**  
   **Type:** `text`  
   **Prop:** `rideComment`  
   **Message:**
    - Provide additional info to the driver  
      **Routes:**
    - `re:.* → AVAILABLE_DRIVERS`

---

7. **AVAILABLE_DRIVERS**  
   **Type:** `list`  
   **Prop:** `driver`  
   **Message:**
    - **Title:** Available Drivers
    - **Body:** I found rides matching your choice. Select one you like, and they will be ready to pick you up.
    - **Footer:** PickDrive
    - **Button:** Rides
    - **Sections:**  
      **Available Rides:**
        - **0:**
            - **Title:** Donald | $3.50
            - **Description:** Honda Fit, 300m - (4.0 ⭐)
        - **1:**
            - **Title:** Pamela | $3.20
            - **Description:** Toyota Aqua, 300m - (3.8 ⭐)
        - **2:**
            - **Title:** Tanaka | $3.00
            - **Description:** Honda Fit, 250m - (4.2 ⭐)
        - **3:**
            - **Title:** Constance | $4.00
            - **Description:** Nissan Note, 100m - (2.8 ⭐)
        - **4:**
            - **Title:** Mr Smith | $3.00
            - **Description:** Toyota Vitz, 320m - (3.2 ⭐)  
              **Routes:**
    - `re:.* → CONFIRM_OFFER`

---

8. **CONFIRM_OFFER**  
   **Type:** `button`  
   **Message:**
    - **Title:** PickDrive Ride
    - **Body:**
      ```
      Driver: Donald  
      Vehicle Color: Silver  
      Reg Number: QZW 4q5C  
      ```  
      Confirm to notify the driver to pick you up now
    - **Buttons:** [Confirm, Abort]  
      **Routes:**
    - `confirm → DRIVER_COMMAND_COME`
    - `abort → PICKUP_DRIVE`

---

9. **DRIVER_COMMAND_COME**  
   **Type:** `button`  
   **Message:**
    - **Title:** PickDrive
    - **Body:** Driver is coming to your location now
    - **Buttons:** [Rate]  
      **Routes:**
    - `rate → START_MENU`

---


This formatting highlights the structure of the flow, making it easier to follow each step, message, and route.
