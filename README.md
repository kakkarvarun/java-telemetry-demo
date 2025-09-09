Java Telemetry Demo (Micrometer + Prometheus) — GitHub Codespaces

A simple Java app that exposes a Prometheus /metrics endpoint using Micrometer.
Runs entirely in GitHub Codespaces (no Docker needed) and can be visualized with Prometheus + Grafana.

You’ll do everything inside Codespaces in your browser.
When the README says click Ports → globe, that means use the Ports tab at the bottom of Codespaces and click the globe icon to open the forwarded URL.

Table of contents

Prerequisites

Create/Prepare the repository

Open in GitHub Codespaces

Project structure (folders & files)

Add Maven config (pom.xml)

Add the Java app (TelemetryExample.java)

Run the app & check /metrics

Install & run Prometheus (no Docker)

Install & run Grafana (no Docker) – optional

Clean finish (best practices)

Quick restart next time

Troubleshooting

Folder layout

Prerequisites

GitHub account (logged in).

GitHub Codespaces access (enabled on your account/org).

Basic knowledge of copy/paste and using a terminal (we’ll guide you).

Create/Prepare the repository

Skip this if your repo already exists.

Go to https://github.com
 → top-right + → New repository.

Repository name: java-telemetry-demo

Description: Java telemetry demo with Micrometer & Prometheus (Codespaces)

Choose Public (recommended for learning).

Check Add a README file (optional).

Click Create repository.

Open in GitHub Codespaces

Open your repo on GitHub → click the green Code button.

Click the Codespaces tab → Create codespace on main.

Wait for the browser-based VS Code to load (this is your dev machine in the cloud).

Project structure (folders & files)

If these already exist in your repo, skip creating them again.

In the Explorer (left sidebar):

Right-click the repo name → New Folder → name it src.

Right-click src → New Folder → main.

Right-click main → New Folder → java.

At the repo root, create a file named .gitignore (exact name, starts with a dot).
Paste and save:

# Maven / Java
target/
*.class
*.log
.classpath
.project
.settings/

# Editor/OS
.vscode/
.idea/
*.iml
.DS_Store
Thumbs.db

# Local tools (Prometheus/Grafana binaries live here)
tools/

Add Maven config (pom.xml)

At the repo root, create pom.xml and paste:

<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>java-telemetry-demo</artifactId>
  <version>1.0-SNAPSHOT</version>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-core</artifactId>
      <version>1.11.4</version>
    </dependency>
    <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-registry-prometheus</artifactId>
      <version>1.11.4</version>
    </dependency>
  </dependencies>
</project>


Save the file (Ctrl+S / Cmd+S).

Add the Java app (TelemetryExample.java)

Create src/main/java/TelemetryExample.java and paste:

import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.util.Random;

// Tiny HTTP server
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class TelemetryExample {
    public static void main(String[] args) throws Exception {
        // Create a Prometheus registry
        PrometheusMeterRegistry registry =
            new PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT);

        Random random = new Random();

        // A gauge whose value is evaluated on each scrape (20..29)
        Gauge.builder("temperature_celsius", () -> 20 + random.nextInt(10))
             .description("Temperature in Celsius")
             .register(registry);

        // Minimal HTTP server exposing /metrics on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/metrics", http -> {
            String response = registry.scrape();
            byte[] bytes = response.getBytes();
            http.getResponseHeaders()
                .add("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            http.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = http.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.setExecutor(null);
        server.start();

        // Keep the program running so Prometheus can scrape it
        Thread.sleep(Long.MAX_VALUE);
    }
}


Save the file.

Run the app & check /metrics

In Codespaces, Terminal → New Terminal.

Run:

mvn clean compile
mvn exec:java -Dexec.mainClass=TelemetryExample


Leave this terminal running (this is Terminal #1).

Click the Ports tab (bottom). If you don’t see it: View → Ports.

You should see 8080. If not, click Forward a Port → type 8080 → Enter.

In the Ports tab, in the row for 8080, click the globe icon → a browser tab opens.

In that browser tab, add /metrics at the end of the URL and press Enter.
You should see text like:

# HELP temperature_celsius ...
# TYPE temperature_celsius gauge
temperature_celsius 23

Install & run Prometheus (no Docker)

We’ll download Prometheus as a binary into a tools/ folder (ignored by Git).

Terminal → New Terminal (so the app keeps running). This is Terminal #2.

Run these commands exactly:

cd /workspaces/java-telemetry-demo
mkdir -p tools && cd tools
curl -LO https://github.com/prometheus/prometheus/releases/download/v2.54.1/prometheus-2.54.1.linux-amd64.tar.gz
tar xzf prometheus-2.54.1.linux-amd64.tar.gz
cd prometheus-2.54.1.linux-amd64

# Create Prometheus config to scrape our app on localhost:8080
cat > prometheus.yml << 'EOF'
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: 'java-telemetry-demo'
    static_configs:
      - targets: ['localhost:8080']
EOF

# Start Prometheus on port 9090
./prometheus --config.file=prometheus.yml --web.listen-address=:9090


Back in Ports, you’ll see 9090. Click the globe to open Prometheus UI.

In Prometheus:

Left: Status → Targets → your job should be UP.

Top: Graph → type temperature_celsius → Execute → Graph to see the line.

Install & run Grafana (no Docker) – optional

Terminal → New Terminal (Prometheus stays running). This is Terminal #3.

Run:

cd /workspaces/java-telemetry-demo/tools
curl -LO https://dl.grafana.com/oss/release/grafana-11.1.0.linux-amd64.tar.gz
tar xzf grafana-11.1.0.linux-amd64.tar.gz
cd grafana-11.1.0
./bin/grafana-server web


In Ports, open 3000 via the globe.

Login: admin / admin (set a new password when asked).

Add Prometheus data source:

Left sidebar ⚙️ Settings → Data sources → Add data source → pick Prometheus.

URL: http://localhost:9090

Click Save & test → it should say Data source is working.

Create a dashboard panel:

Left + → Dashboard → Add visualization → choose Prometheus.

Query: temperature_celsius → Run query → Apply (top-right).

Clean finish (best practices)

We’ll make a clean commit on a branch, open a PR, and merge it to main.

1) Make sure tools/ isn’t going into Git

Explorer → ensure .gitignore contains tools/.

Source Control (branch icon) → Changes should NOT show anything from tools/.
If it does:

git rm -r --cached tools


Then commit again.

2) Create a new branch

Bottom-left branch name → Create new branch…

Name it: feat/metrics-endpoint → Enter.

3) Stage & commit

Source Control → + to stage your changed files (TelemetryExample.java, pom.xml, .gitignore, README.md).

Commit message:

feat(metrics): expose Prometheus metrics at /metrics on port 8080


Click ✔ Commit.

4) Push → Pull Request → Merge

Source Control → … → Push (or Publish Branch).

Click Create Pull Request (or on GitHub: Compare & pull request).

PR title:

feat(metrics): expose Prometheus metrics at /metrics on port 8080


PR description:

- Implement /metrics HTTP endpoint using Micrometer Prometheus registry
- Keep app running for Prometheus scraping
- Add/confirm .gitignore to exclude build outputs and local tools
- Provide README with step-by-step Codespaces instructions


Create pull request → Merge pull request → Confirm merge → Delete branch.

5) Switch back to main & pull

Bottom-left branch name → select main.

View → Command Palette → “Git: Pull”.
(Or terminal: git fetch origin && git checkout main && git pull --ff-only origin main)

Quick restart next time

App (Terminal #1):

mvn exec:java -Dexec.mainClass=TelemetryExample


Prometheus (Terminal #2):

cd /workspaces/java-telemetry-demo/tools/prometheus-2.54.1.linux-amd64
./prometheus --config.file=prometheus.yml --web.listen-address=:9090


Grafana (Terminal #3):

cd /workspaces/java-telemetry-demo/tools/grafana-11.1.0
./bin/grafana-server web


Open UIs via Ports tab:

8080 → add /metrics in the URL

9090 → Prometheus UI

3000 → Grafana UI

Troubleshooting

I opened http://localhost:8080 on my laptop and it doesn’t work
Use the Ports tab → click the globe next to the port.
Codespaces is remote; the correct URL ends with .app.github.dev.

Prometheus Target is DOWN
Make sure the app is running (Terminal #1) and /metrics opens in your browser (Ports → 8080 → /metrics).
In prometheus.yml, the target must be localhost:8080 (Prometheus runs in the same Codespace).

Grafana says “401 Unauthorized”
In the data source, the URL must be http://localhost:9090.
Do not use the forwarded .app.github.dev URL or /graph path.

Ports tab doesn’t show a port
Click Forward a Port and type the port number (8080/9090/3000) → Enter → then click the globe.

Folder layout
java-telemetry-demo/
├─ src/
│  └─ main/
│     └─ java/
│        └─ TelemetryExample.java
├─ tools/                      # Prometheus & Grafana binaries (ignored by Git)
│  ├─ prometheus-2.54.1.linux-amd64/
│  │  ├─ prometheus            # run with config file
│  │  └─ prometheus.yml        # scrapes localhost:8080
│  └─ grafana-11.1.0/
│     └─ bin/grafana-server    # run Grafana
├─ .gitignore
├─ pom.xml
└─ README.md