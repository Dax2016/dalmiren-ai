# Physical Android Device Testing Guide

## Step-by-Step Instructions

1. **Start the FastAPI Backend**:
   ```bash
   cd backend
   pip install -r requirements.txt
   uvicorn main:app --host 0.0.0.0 --port 8000 --reload
   ```

2. **Discover Host Computer Wi-Fi IPv4 Address**:
   - On Windows: run `ipconfig` (look for `IPv4 Address` under Wireless LAN adapter Wi-Fi, e.g., `192.168.1.105`).

3. **Configure Android App for Physical Phone**:
   - In `app/src/main/java/com/darchums/dalmiren/data/remote/NetworkClient.kt`, set the base URL to your computer's IP:
     ```kotlin
     NetworkClient.setCustomBaseUrl("http://192.168.1.105:8000/")
     ```

4. **Connect Phone and Run**:
   - Ensure the physical Android phone is on the same Wi-Fi network as your computer.
   - Deploy and launch DALMIREN AI on the phone.

5. **Verify AI Companion**:
   - Send queries from the phone and observe live FastAPI logs showing backend request processing, automatic provider routing, and persistent memory updates.
