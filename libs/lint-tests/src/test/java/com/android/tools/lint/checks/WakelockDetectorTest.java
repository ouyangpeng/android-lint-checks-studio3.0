/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class WakelockDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new WakelockDetector();
    }

    public void test1() throws Exception {
        String expected = ""
                + "src/test/pkg/WakelockActivity1.java:15: Warning: Found a wakelock acquire() but no release() calls anywhere [Wakelock]\n"
                + "        mWakeLock.acquire(); // Never released\n"
                + "                  ~~~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                mOnclick,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Bundle;\n"
                        + "import android.os.PowerManager;\n"
                        + "\n"
                        + "public class WakelockActivity1 extends Activity {\n"
                        + "    private PowerManager.WakeLock mWakeLock;\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "        mWakeLock.acquire(); // Never released\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity1.class", ""
                        + "H4sIAAAAAAAAAIWS328SQRDHv8uvA3pKhYoWqwK2FWjtqS8+YEyUxISEqgkN"
                        + "Pi/HBrcce3i30PTPMlGa+GB89o8yzh6QNrGkL7M/ZuYz35ndP39//gLwEi8s"
                        + "xBhKWoTamYyGzmc+Ep7vjt66Ws6kPid/gmGLq0Hgy4HDJxNn5WLIjE14h8IZ"
                        + "qp1VjB86n/wzERxzxYci2F3FNBlSr6WS+g1DvFbvMSRa/kBkEceGDQtphlxH"
                        + "KvFhOu6L4IT3PcGQp0zu9XggzXl5mdBfZMiw01kvm4qlfdUKBNeUUKxdVfdu"
                        + "qgaeaNZ7pnTBxibupHGXITkxsrNguGfjPrYZNodCd89DLcZdEcykS6j9WueU"
                        + "z7jjcTV0ujqQatisX7n62D8Vrm5aeECYNSNJ4yE1cULisyjhsY0yKgwbSpxd"
                        + "zvNVrX1doRuHnCH5T2wkkcpiD08t7DOUb8qyUTPjt7j7dSoDM/aQz8SgrULN"
                        + "lSu6Oppi4ZohUtJ4AWPYXqeOorJdfxq44r0071f877mOTKsMdlspEbQ8HoaC"
                        + "Hji90ocKdRWnD8sQM63RLkZ7+jNkM3RyaGW0JhsXyH6L3DbZVHRp4RZZexGA"
                        + "28hFIHr1ZfIEiQhebRxcIN/Ib81R/I3SceMwkd+Z49EP8nxHdY7dS3KRSIaZ"
                        + "JHaOmCWyFWKaSuUFbVnJ7OpoUK0cCjjAIRFieBaRjkg4YH7cHhGeg/0DZ4fC"
                        + "upgDAAA="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expect(expected);
    }

    public void test2() throws Exception {
        String expected = ""
                + "src/test/pkg/WakelockActivity2.java:13: Warning: Wakelocks should be released in onPause, not onDestroy [Wakelock]\n"
                + "            mWakeLock.release(); // Should be done in onPause instead\n"
                + "                      ~~~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                mOnclick,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.PowerManager;\n"
                        + "\n"
                        + "public class WakelockActivity2 extends Activity {\n"
                        + "    private PowerManager.WakeLock mWakeLock;\n"
                        + "\n"
                        + "    @Override\n"
                        + "    protected void onDestroy() {\n"
                        + "        super.onDestroy();\n"
                        + "        if (mWakeLock != null && mWakeLock.isHeld()) {\n"
                        + "            mWakeLock.release(); // Should be done in onPause instead\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    protected void onPause() {\n"
                        + "        super.onDestroy();\n"
                        + "        if (mWakeLock != null && mWakeLock.isHeld()) {\n"
                        + "            mWakeLock.release(); // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity2.class", ""
                        + "H4sIAAAAAAAAAKVRTUtbQRQ9N4l5yXNsNOajtUnVmBZ10QduK4VikRZiERQL"
                        + "xc2YDDrm+SbMe1H8Kf4MobTgwmUX/ijxTvKCi6JddHPvnTvn3HPuzN39zS2A"
                        + "DbzzkCEsJCpOgkH/OPgu+yo03f6nbqLPdXK54SFHqMioZ43uBXIwCCZXhOKZ"
                        + "g3cYTmh1JhgTB7vmQtkdGcljZdsTzAdCflNHOvlIyK6uHRByW6anfGQxLeCh"
                        + "QCh1dKS+Dc+OlN2XR6EilJkpwwNptTunzVxyomNCo/O0bRYrmugz31tz6STK"
                        + "ArMoFMHLCEwh76OGlx7qhKV/GRd4hQU2r+MvKuyNzP9w9IZA07n2rAqVjNnY"
                        + "zF4iu/0dOUiNeibalUN34++Zoe2qbe3atb/svj+V55IgvkaRsluhjGMVe2gT"
                        + "6k+YIxQm9rDMS2X5NwkZtxlXWa75QTkW+RRwJs5T67/hX3ORgeCYHzXzmOEo"
                        + "xgC8QIlzzj1VSj5MyU0mz63/xPwfVFz6heoV/HH1+nqk6YaWmQxMcxSo8ugG"
                        + "D3wUaKYCb8YeGwxb/D+leY4VVqqyUv05pQyWRpsvo8XZZ1ANK3gLegC314lj"
                        + "CwMAAA=="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expect(expected);
    }

    public void test3() throws Exception {
        String expected = ""
                + "src/test/pkg/WakelockActivity3.java:13: Warning: The release() call is not always reached [Wakelock]\n"
                + "        lock.release(); // Should be in finally block\n"
                + "             ~~~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                mOnclick,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.PowerManager;\n"
                        + "\n"
                        + "public class WakelockActivity3 extends Activity {\n"
                        + "    void wrongFlow() {\n"
                        + "        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "        PowerManager.WakeLock lock =\n"
                        + "                manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "        lock.acquire();\n"
                        + "        randomCall();\n"
                        + "        lock.release(); // Should be in finally block\n"
                        + "    }\n"
                        + "\n"
                        + "    static void randomCall() {\n"
                        + "        System.out.println(\"test\");\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity3.class", ""
                        + "H4sIAAAAAAAAAIWTbU8TQRDH/1tK73qcQsuTRUBExPJ4CipqkcQ0ISEpSlKC"
                        + "r5djU5de7+reloaPpS+q8YXxtR/KOHulQmIbLuk+zM38ZuZ/099/fvwEsIU9"
                        + "CymGGS1i7TXrNe8jr4sg8uvvfC0vpL7ctpBmmODhmYrkmcebTa/3iiGzK0Op"
                        + "9xiGiisnDOlydCYcDCHrYhgZhtGKDMX7VuNUqGN+GgiGfCXyeXDClTT3K2Na"
                        + "f5Ixw2xlcBUlhmxbRWFtP4jaNsYYhptRWygHxHQxjgmGsZrQ1ctYi0ZVqAvp"
                        + "E3m5WDnnF9wLeFjzqlrJsFZauWH6cHoufF2yMMUw3esxir0jwz7kIa8JZeMe"
                        + "lXhMpTmYxIyL+5hlGAlF21RJ/dQZdooH/RINIC71AksO5rFg4QHDwm2+Lh4a"
                        + "SS3uf25JJUzjj1wsGZujKDhqlHkQGOCyiyeJqxKB4DHJYDW6MIbCoJpI4HSQ"
                        + "9LJ4e9lZbMCzsEma3+g6kd7FUzyjgYhammGyK4qMvCNSRJMugjdKNrbNN08E"
                        + "fYEdCy8Zxvs4uniF11R805iCkHD9vibNnVONWsoX+9IM09R/s7NpghjcgzAU"
                        + "qhzwOBY0bXavG9KV6oV5huhEg0urRTePdkb78Oo32F/okIJDayYxZjBCq9t1"
                        + "oP1OArj7L1gTzEDnV/OjHeR+kRZr6fx0B4XD9Q7mvmORtsfX1Pkkyqazgxyy"
                        + "yBN/irgFIs8R12Rb6BKvsjmYQBEr5g9AllWsEck2A3FVwQbdzZP9ivX8VgfP"
                        + "r5M5yascuecTcCr5vUnWEnYTD0aJJvEW7C81D3fKKwQAAA=="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expect(expected);
    }

    public void test4() throws Exception {
        String expected = ""
                + "src/test/pkg/WakelockActivity4.java:10: Warning: The release() call is not always reached [Wakelock]\n"
                + "        getLock().release(); // Should be in finally block\n"
                + "                  ~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                mOnclick,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.PowerManager;\n"
                        + "\n"
                        + "public class WakelockActivity4 extends Activity {\n"
                        + "    void wrongFlow2() {\n"
                        + "        getLock().acquire();\n"
                        + "        randomCall();\n"
                        + "        getLock().release(); // Should be in finally block\n"
                        + "    }\n"
                        + "\n"
                        + "    private PowerManager.WakeLock mLock;\n"
                        + "\n"
                        + "    PowerManager.WakeLock getLock() {\n"
                        + "        if (mLock == null) {\n"
                        + "            PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "            mLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "        }\n"
                        + "\n"
                        + "        return mLock;\n"
                        + "    }\n"
                        + "\n"
                        + "    static void randomCall() {\n"
                        + "        System.out.println(\"test\");\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity4.class", ""
                        + "H4sIAAAAAAAAAI1T708TQRB925ZeexwCx29EBURsi3CKKGoRxCqRpChJCX5e"
                        + "jk09er2rd1sa/is1Wo0fjN9M/KOMs9cWmgjBu9zu3uyb92ZmZ3//+f4DwDJe"
                        + "aogxTEoRSqtWKVtveUW4vl3ZtKVz7MiTFQ0JhmHuHQa+c2jxWs3qbDH0VIsE"
                        + "ZZgtdvb90Nr1GyLY4R4vi2BO0SlMniG55niOXGeIZ7L7DImCfyh0xNFrQEOK"
                        + "ob/oeOJ1vXoggj1+4AoGkzy5u88DR/23jQn5zgkZpooXh0xieiPwvfKW6zeW"
                        + "dRCTgSEMM2hlIVshz2WylwetYxTjGsYYpi/DGphQSWjcfl93AqFErxqYUjY9"
                        + "IGe/WuCuqwivG7gRQQPhCh6KNEFnDPQgmcJNqmlN0Sv/WwbmcZthgIIunYRS"
                        + "VEsiOHZsKsJ8pnjEj7nlcq9slWTgeOV8tsv05uBI2DKvIcswdkHoKSxQNfeo"
                        + "ijoyWDSwBIuh1xONTk4Mq5nt84T+47S1asvGMHERnFB9Jcntyg6vRYebxgoe"
                        + "anhAKXeJRpkbWMUjah2/LhlGWjE5vrVLAUkKS/BqPoUnqjuifNawruEpw9A5"
                        + "QAMbeEYB1pTJ9YjuvGJSh+olvx7YYstRbTf6T5ctKScGY9vzRFBweRgK6stU"
                        + "pwaYoSOM0x1jiKnDpVWC1tTqNKbpz6KZ0dyT+wr9Iy1iMGhMRsYk+mg0WgBc"
                        + "QT/UM3Dq/KrtbOaaGGxi5BsmW6trZ0wmCYI8NPLQ6R1EbxerecpKN6PNekQe"
                        + "MZpncp8x/QvjOXO2ibmfyBRzCwkldecLptXeBwLFu0QGSGSQXpM6fiQSGW0R"
                        + "tUWU9F3cI5nlyJPRxVCXo627GIGB9CfcNx83kT9LQo+26G5hMuKNRd9mND5H"
                        + "IULQ6VAHvwD7C39Pxt3WBAAA"))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expect(expected);
    }

    public void test5() throws Exception {
        String expected = ""
                + "src/test/pkg/WakelockActivity5.java:13: Warning: The release() call is not always reached [Wakelock]\n"
                + "        lock.release(); // Should be in finally block\n"
                + "             ~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                mOnclick,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.PowerManager;\n"
                        + "\n"
                        + "public class WakelockActivity5 extends Activity {\n"
                        + "    void wrongFlow() {\n"
                        + "        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "        PowerManager.WakeLock lock = \n"
                        + "                manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "        lock.acquire();\n"
                        + "        randomCall();\n"
                        + "        lock.release(); // Should be in finally block\n"
                        + "    }\n"
                        + "\n"
                        + "    static void randomCall() {\n"
                        + "        System.out.println(\"test\");\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity5.class", ""
                        + "H4sIAAAAAAAAAIWTbU8TQRDH/wuldz1OoYUWQUBExFIeTlFELZIYEhKSoiQl"
                        + "+Hp7bOrS613d27bhY+mLanxhfO2HMs5eqZAI4ZLuw9zMb2b+N/3958dPAJvY"
                        + "tTDEMKNFrL1Wo+595A0RRH7jna9lR+rzLQsphkkenqpInnq81fIGrxjSOzKU"
                        + "epdhuLhywpDai06Fg2FkXIwgzTBWkaF4327WhDrmtUAw5CqRz4MTrqS5XxhT"
                        + "+pOMGWYrN1dRZsh0VRTW94Ooa2OcYaQVdYVyQEwXE5hkGK8LXT2PtWhWhepI"
                        + "n8jLxcoZ73Av4GHdq2olw3p55YrpQ+1M+LpsocAwNegxir0jwz7kIa8LZeMe"
                        + "lXhMpTnIY8bFfcwyjIaia6qkfhoM28WD6xLdQFwaBJYdzGPBwgOGhdt8XTw0"
                        + "klrc/9yWSpjGH7lYMjZHUXDU3ONBYIDLLp4krkoEgsckg9Xswximb6qJBE4F"
                        + "SS+Lt5edwTo8Cxuk+ZWuE+ldPMUzGoiorRnyfVFk5B2RIpp0EbxZtvHcfPNE"
                        + "0C1sW3jJMHGNo4tXeE3Ft4wpCAl33dekuXOqUVv5Yl+aYSr8NzsbJojBPQhD"
                        + "ofYCHseCps0edEO6Ur0wzzCdaHBptejm0c5oHyl9g/2FDkNwaE0nxjRGaXX7"
                        + "DrTfSQB3/wVrghnofCk31kP2F2mxmspN9TB9uNbD3Hcs0vb4kjqfRNl0dpBF"
                        + "BjniF4g7TeQ54ppsC33iRTYHkyhixfwByFLCKpFsMxAXFazT3TyZr1jLbfbw"
                        + "4jKZk7zKknsuAQ8lvzfJWsZO4sEoUR5vwf4Cdgr9nisEAAA="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expect(expected);
    }

    public void test6() throws Exception {
        String expected = ""
                + "src/test/pkg/WakelockActivity6.java:19: Warning: The release() call is not always reached [Wakelock]\n"
                + "            lock.release(); // Wrong\n"
                + "                 ~~~~~~~\n"
                + "src/test/pkg/WakelockActivity6.java:28: Warning: The release() call is not always reached [Wakelock]\n"
                + "            lock.release(); // Wrong\n"
                + "                 ~~~~~~~\n"
                + "src/test/pkg/WakelockActivity6.java:65: Warning: The release() call is not always reached [Wakelock]\n"
                + "        lock.release(); // Wrong\n"
                + "             ~~~~~~~\n"
                + "0 errors, 3 warnings\n";
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                mOnclick,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import com.example.test3.BuildConfig;\n"
                        + "\n"
                        + "import android.annotation.SuppressLint;\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.PowerManager;\n"
                        + "import android.os.PowerManager.WakeLock;;\n"
                        + "\n"
                        + "public class WakelockActivity6 extends Activity {\n"
                        + "    void wrongFlow1() {\n"
                        + "        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "        PowerManager.WakeLock lock =\n"
                        + "                manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "        lock.acquire();\n"
                        + "        if (getTaskId() == 50) {\n"
                        + "            randomCall();\n"
                        + "        } else {\n"
                        + "            lock.release(); // Wrong\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    void wrongFlow2(PowerManager.WakeLock lock) {\n"
                        + "        lock.acquire();\n"
                        + "        if (getTaskId() == 50) {\n"
                        + "            randomCall();\n"
                        + "        } else {\n"
                        + "            lock.release(); // Wrong\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    void okFlow1(WakeLock lock) {\n"
                        + "        lock.acquire();\n"
                        + "        try {\n"
                        + "            randomCall();\n"
                        + "        } catch (Exception e) {\n"
                        + "            e.printStackTrace();\n"
                        + "        } finally {\n"
                        + "            lock.release(); // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public void checkNullGuard(WakeLock lock) {\n"
                        + "        lock.acquire();\n"
                        + "        if (lock != null) {\n"
                        + "            lock.release(); // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    @SuppressLint(\"Wakelock\")\n"
                        + "    public void checkDisabled1(PowerManager.WakeLock lock) {\n"
                        + "        lock.acquire();\n"
                        + "        randomCall();\n"
                        + "        lock.release(); // Wrong, but disabled\n"
                        + "    }\n"
                        + "\n"
                        + "    void wrongFlow3(WakeLock lock) {\n"
                        + "        int id = getTaskId();\n"
                        + "        lock.acquire();\n"
                        + "        if (id < 50) {\n"
                        + "            System.out.println(1);\n"
                        + "        } else {\n"
                        + "            System.out.println(2);\n"
                        + "        }\n"
                        + "        lock.release(); // Wrong\n"
                        + "    }\n"
                        + "\n"
                        + "    static void randomCall() {\n"
                        + "        System.out.println(\"test\");\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity6.class", ""
                        + "H4sIAAAAAAAAAIVV31cbRRT+JiRssmwKDeVnKQVKIPxMoQW0QCulUFMCxQap"
                        + "2qchGcM2m924uwH7H/gX+OzxQZ71IXp8UJ988G9Sj3cmyRIsHHLOzszOvXu/"
                        + "7373zuSvf3/9DcAi3mgIMQz6wvPTlVIx/ZqXhOXkSxt53zwx/XfLGsIMt7hd"
                        + "cB2zkOaVSrppYmhfM23Tf8zQlpo6ZAhvOgWhow0xAxG0M3RmTVvsVctHwj3g"
                        + "R5ZgSGSdPLcOuWvK98Zm2D82PYah7NUsVhn0U9exi9uWc7oQRRdDpOKcClcH"
                        + "BTXQjVsMXUXh5955vijnhHti5in0RCr7lp/wtMXtYjrnu6ZdXJ1q2Xp59Fbk"
                        + "/VUNvQx9zSQdL70vY+9ymxeFG0U/cTwgbjp6MGjgNoYYOmxxKmlSQiWGlVTm"
                        + "MqArIo43P1zVMYwRDXcZRq7zNTAqNdV4/quq6QqZ+D0D40gyxCjxA+6VMgVV"
                        + "ioy0TRpISX/dpcBOeZNblgSbNjCjwrjCEtwjibRyHYhh4Cq+pH7YUnmOXZ8S"
                        + "Qzzn83xpl1ca9T2v3CJDMnV9CNlMmlNStdaxhBUNywzd5wJvfZ0XFd90bAMf"
                        + "qEarkOC+gj1wuSw8o6cne8kXVOu1C7EOjl3ntE70Rv5Y5Et7Vct6XuVuobnx"
                        + "zPSkvbDAcPtV1fbNssjYJ6Zn0uaGbTs+l4Gpg0eD1Hiwnc5VKxVXeB6dBZ/E"
                        + "iZxwq0pY0WaPt+rzIIYtPNewTc3c0k6qpw18jAzV16n6QWqmk96vZ+4KXqZu"
                        + "2sGuhmwzv4tmA3t4ScIqrSybaprKSKVDJmXKMlHk5FFUbb6DT6U3GXsuO0C0"
                        + "r+ecqpsX26bUrfe98zovP2IwMrYt3E2Le57wGjnLClMrUyaQvyit6LKgUaO3"
                        + "NM2M5sj0z4j+SIsQdBrb1aaODhqNugPNcRXgRvDxtxRSBp2eTnTWcPMPUmkm"
                        + "nOirYWB3toY70zWMdS1+h9gvmDiDRltTEqFNIYwRERlbp3g3KXICnehFF+7S"
                        + "W5Kulyk6+hJ9pI7QQNfpfRZz8g5CP+aJfgj3VZzQP0hq6NEwLP0X6Katc/yS"
                        + "PEI098+8z2jmAqNeypH8EMYAYQ0ShyH00QnuUDYVI9Cgjs0Udhihbon5IMD8"
                        + "geawzFFiKqheKchDiXeGG3tynvu9jh4iTw3y0IWVAk0202ocpd17tBonFkmy"
                        + "T5LHBLGapDsxhTs0j5JS5zqNBQzHGgw7KMKHeBToFH6hYWlHw2p7G1nXA86H"
                        + "NMsc45LzzJ//FyehEpqlcY4KN0+B7rcIEw9g4y3CEIDsnceEEcIT1TkMH9Gz"
                        + "8QbMw1OFm23gGg2tmrChFtiHNC6R9zJBrrTAGgGs0YAFNoOMviEfqUlSVn1d"
                        + "hh/qWvwe8Z/wLFzDizPotIrQ6mKmw+rjR9QMq4hhjURfp4Z4QgJvUBmetoid"
                        + "DOCTDfgIWfbxSUNs+qf/GwOsnQRLBWdmTjEHYgSeeFXDwXmyujJtkfu2Qgmp"
                        + "57UaP8PnyoMRvx58AfYfMhflwVEIAAA="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expect(expected);
    }

    public void test7() throws Exception {
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                mOnclick,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.os.PowerManager.WakeLock;\n"
                        + "\n"
                        + "public class WakelockActivity7 {\n"
                        + "    public void test(WakeLock lock) {\n"
                        + "        try {\n"
                        + "            lock.acquire();\n"
                        + "            new Runnable() {\n"
                        + "                public void run() {\n"
                        + "                }\n"
                        + "            };\n"
                        + "        } finally {\n"
                        + "            lock.release();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity7.class", ""
                        + "H4sIAAAAAAAAAIVSXU8TQRQ9F0qnrNtSWlukonwVLUrcwIsPGBNDYmLSoglN"
                        + "eZ5uJ2XYdRdntxDjD/KZF0wgwXd/FPHOtsYHU9lN7sfMOWfO7N1fd9e3APbQ"
                        + "EpghNFKVpN5ZMPSOZaDC2A/e+ak+1+nX1wI5QvlUnksvlNHQ+9g/VX5KyL/R"
                        + "kU7fEmZb2z1C7iAeKAezmHcxhzxhoa0jdTj63FemK/uhIlTasS/DnjTa9pPF"
                        + "XHqiE8JKe7qDfYviXcJWqy2jgYn1wIsT71N8oUxHRnKoTNOyWD/Y3+45WERV"
                        + "oEJYuw/t4qH1KqT/ZaSNEqizlelOmrsOanhkb7hMWG39z/TYx2MXK9kJRoVK"
                        + "JvbCFkbYuP8mhOJRKv2gI8+yjyWwTqj+nUT3xMQX46/oHMUj46v32jb1f8y8"
                        + "siSC+yGKlDkIZZKoROAZYWmKCULhjw2sg2cM+8xwxbPlKLjzOBPnuRc/ULjM"
                        + "th2O+fEiHnB0J7WLImdCCQsT8jdGW9HGyyuUb1BjjaXvcDrcNnZ+2niZcUv8"
                        + "gpFW+WnGLCCHeV4tslYJVY41zssoZyfWWZdVJyfa6gnzeFZc859+KLCWZ8hG"
                        + "ZncTTc4VrhaxhecMq2Uk/AYaMMabHgMAAA=="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expectClean();
    }

    public void test8() throws Exception {
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Bundle;\n"
                        + "import android.os.PowerManager;\n"
                        + "import android.os.PowerManager.WakeLock;\n"
                        + "\n"
                        + "import com.google.io.demo.R;\n"
                        + "\n"
                        + "public class WakelockActivity8 extends Activity {\n"
                        + "\tprivate WakeLock mWakeLock;\n"
                        + "\n"
                        + "\t@Override\n"
                        + "\tprotected void onCreate(Bundle savedInstanceState) {\n"
                        + "\t\tsuper.onCreate(savedInstanceState);\n"
                        + "\t\tsetContentView(R.layout.main);\n"
                        + "\t\tPowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "\t\tmWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "\t\tmWakeLock.acquire();\n"
                        + "\t\tif (mWakeLock.isHeld()) {\n"
                        + "\t\t\tmWakeLock.release();\n"
                        + "\t\t}\n"
                        + "\t}\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity8.class", ""
                        + "H4sIAAAAAAAAAIVTW08TQRT+phdaluViuQmiLYjQFmQFL6BFEm1ibNKqSUlN"
                        + "fBu2k7p0O1t3pyU8+Tt89CeYaEl8MD77m4zxzLaNJEJ4mXOZ78x3bvPrz/cf"
                        + "AHZwkECEYVGJQFntZsN6y5vC9ezmM1s5XUed7iUQY5jhsu57Tt3i7bY1vGIY"
                        + "bWl4meAMK+UhxgusN96J8Ctc8obwV4eYAsPIviMddcAQzeZqDLGiVxcGohgz"
                        + "kUCSYbLsSPGq0zoS/iE/cgVDiiK5W+O+o+2BM6beOwHDUvnytIks6cmiL7ii"
                        + "gLns+eyed2TdFYVcTVNPm5jCtejHKEYMENLEPK4zTARCFT2phFQ1R5wQZ7aU"
                        + "qyWxyBBv6+o0eMnETdximGoIVT0NlGhVhd91bGJcy5aPeZdbLpcNq6p8RzYK"
                        + "uXOu10fHwlaFBDIM85d0LokV4j2kGg2ksWriDtYYxqQ4+df23WzpIqIrZzFK"
                        + "6WdNxHXVeWwmsMGQuSrKxF09pQS3P3QcX+hIy8Q9bNNkneClcOvhZN/pi/sm"
                        + "HoRgX7iCB3qUAe+KekkGiktbVFU4mekLBkNBrT4zw8JlpRBqnN6wmxXeHqyF"
                        + "UfU6vi1eONqY+28ltnSfGMySlMIvujwIBC1RclgclqklUfoUDBHdF9IipNNe"
                        + "0jlKlkWSkYznz2B8Ca9NOkdCp4FxOs0+ABOYJBnTmzUI/kyWfvxxfuMMqXxq"
                        + "pofZfGqhhxs/ka7kN2Op5R5uf8N6/ivWe8j1xdYnGH1tRxNGQ8IVyghEEKfn"
                        + "x4hgFilkMI11zGCbrD3aYJ1Mpk84SEZrD/GI0pnFKnYJFSEPdHW/Qb88TfaT"
                        + "sKgC9sOCGE0xjadgfwFO/216LQQAAA=="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expectClean();
    }

    public void test9() throws Exception {
        // Regression test for 66040
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Bundle;\n"
                        + "import android.os.PowerManager;\n"
                        + "import android.os.PowerManager.WakeLock;\n"
                        + "\n"
                        + "public class WakelockActivity9 extends Activity {\n"
                        + "    private WakeLock mWakeLock;\n"
                        + "\n"
                        + "    @Override\n"
                        + "    protected void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "        mWakeLock.acquire(2000L);\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity9.class", ""
                        + "H4sIAAAAAAAAAIVTW28SQRT+htsudCuUIlqsShFboLXrLTEGY6IkJm2omtDg"
                        + "87BMcMsyi7sDTX+WidLEB+OzT/4i4xkuaRNLupvM9ZzvfOc7Z37//fETwFM8"
                        + "NxBhKCgRKnvY79mfeF94vtN/4yh37KqzlwZiDDkuu4Hvdm0+HNqLK4bkQJs3"
                        + "yZyh1FzY+KH90T8VwRGXvCeC8sKmzpB45UpXvWaIVqpthljD74oUolixYMBk"
                        + "SDddKd6PBh0RHPOOJxiy5Mm9Ng9cvZ8fxtRnN2TYbC6nTcFMXzYCwRU55CuX"
                        + "2b0dya4n6tW2Dr1uIYM1EzcZ4kNNOwWGWxZuY4Mh0xOqdRYqMWiJYOw6BLVd"
                        + "aZ7wMbc9Lnt2SwWu7NWrl44+dE6Eo+oG7hDMEklM3KUkjol8CgXct1DEFsOK"
                        + "FKcXer6oHFwV6FqRk0T/gYU4EnFMP+NPCjuoGqgwFK9zt1DDLoPBnS8jN9BS"
                        + "Vw51pbIhH4vugQwVl45oqamq61eISr6DGSbDxjK2ZJVq+aPAEe9cXc/8f+Xb"
                        + "16kzWAdSiqDh8TAUVHBzQRNblGWUkmOI6FRpFaE19RCNSdrZNDOa47VzpL5O"
                        + "ry0aE9NDE6s0WjMD3ECa5pjugrlzSDsN/rC2e45sLZubIP8LhaPaXiy7OcG9"
                        + "7yjVvqGUQ3mC7QvwPLTiq3PINFU2gzKB6mDFGeA8mF7t4RGFS5PXPrGN0P94"
                        + "ivSEHiWgm3CHEJ5h7R9cGENaqwMAAA=="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expectClean();
    }

    public void test10() throws Exception {
        // Regression test for 43212
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Bundle;\n"
                        + "import android.os.PowerManager;\n"
                        + "\n"
                        + "public class WakelockActivity10 extends Activity {\n"
                        + "    @Override\n"
                        + "    public void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "        PowerManager.WakeLock wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "\n"
                        + "        try {\n"
                        + "            wakeLock.acquire();\n"
                        + "            throw new Exception();\n"
                        + "        } catch (Exception e) {\n"
                        + "\n"
                        + "        } finally {\n"
                        + "            wakeLock.release();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/WakelockActivity10.class", ""
                        + "H4sIAAAAAAAAAIVSW08TQRT+pu0yZdlarkVEFKTFtpQuoGgCxkSJRpKCJiUa"
                        + "H6fbSV267K6zWyq/yCcfeIFEE/HZH6WeWSU0QeNucmbmnPNd5vL9x+evANax"
                        + "YSKHOR1uZXHbhIV5joUs7pgwsGiiiBLHkolh3VTEXY4yR4Vh6JHru/FjhnS5"
                        + "8pohsx20JUO+4fpyr3fYkmpftDzKZAN/W0kR07RQbgi/rQK3bQeR/bTntz25"
                        + "pcG5Ziyc7q4IE0yiUCUTHCskzVFnMJtBTznyuaspp9+IrvQCp/vEid0jNz5e"
                        + "W60fiCNh4RryFsYxwWCEQV8qCzZWCTAg+0rnd4UvOlKR7X0ZxRbWsE5iFu4h"
                        + "zzChuWxP+B372QdHhrEb+Bbu69JsTO122O3YVy0wTF7IiDC0LwoMY1c2Taei"
                        + "8Q3CM1g7vi/VtieiSEYM8//wWrxEDBjcf6eC/u+DHu3IuHkcxfKwKdWR61Bq"
                        + "qdy4bG3GyvU7W5WB1MvWgXTiLYYRX/Yv+R+Wd/6G+58x4uHCed9zFUlzJT0p"
                        + "IokFelE56C8Fpi+I4iit5mhkNBrVM7ATmtBBURxKkpwiXeOf1k9IUx54UF0+"
                        + "Q6o6nj5F5huM3VpmfOgUfG/lFNkvGH57BvN8M0OrkY+wNg09mTHOTwg9jUX6"
                        + "h2ksoUZMNdSTMZVoVmFS1O99hOxZ1JXHLCkvUrWESVQwRd2T2ECBorae/knF"
                        + "DMcUR4FjmuM6GMfMC44bWSKdTahvJhImbaJI1MsY+wUhwfySdAMAAA=="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expectClean();
    }

    public void testFlags() throws Exception {
        String expected = ""
                + "src/test/pkg/PowerManagerFlagTest.java:15: Warning: Should not set both PARTIAL_WAKE_LOCK and ACQUIRE_CAUSES_WAKEUP. If you do not want the screen to turn on, get rid of ACQUIRE_CAUSES_WAKEUP [Wakelock]\n"
                + "        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|ACQUIRE_CAUSES_WAKEUP, \"Test\"); // Bad\n"
                + "           ~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;\n"
                        + "import static android.os.PowerManager.FULL_WAKE_LOCK;\n"
                        + "import static android.os.PowerManager.PARTIAL_WAKE_LOCK;\n"
                        + "import android.content.Context;\n"
                        + "import android.os.PowerManager;\n"
                        + "\n"
                        + "public class PowerManagerFlagTest {\n"
                        + "    @SuppressWarnings(\"deprecation\")\n"
                        + "    public void test(Context context) {\n"
                        + "        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);\n"
                        + "\n"
                        + "        pm.newWakeLock(PARTIAL_WAKE_LOCK, \"Test\"); // OK\n"
                        + "        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|ACQUIRE_CAUSES_WAKEUP, \"Test\"); // Bad\n"
                        + "        pm.newWakeLock(FULL_WAKE_LOCK|ACQUIRE_CAUSES_WAKEUP, \"Test\"); // OK\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/PowerManagerFlagTest.class", ""
                        + "H4sIAAAAAAAAAIVSTW/TQBB9ky+nbqCJC23TD5pCaJ02whIXDkFcIlWq5AJS"
                        + "qvS8cVbGTbKO7G0LP4tLkTggzvwoxGw+1FI1YEvzZnZm37zd2V+/v/8A8BpN"
                        + "CxnCjpap9saD0PsYX8vkVCgRyuR4KMIzTljIEcoX4kp4Q6FC70PvQgaaUHgb"
                        + "qUi/I2TdRpeQa8d9aSOLpRLyKBBW/EjJ95ejnkzORG8oCY4fB2LYFUlk4tli"
                        + "Tn+KUsKu/08RLVPISNh2faH6SRz1vSBWWirttQ1+1q1Gt4gKIT82BDZW8dTC"
                        + "E8L6gvoS1rDORwul7nxJtRx1ZHIVBaxp3/Vvz9vRSaTCVsO/fwUtC9U77HH6"
                        + "l/Iitliz0W5jAzslPMMuYVnJ63MxkHwTA8Ib9+ShRgsY6/ONrWwZIGM2CVYw"
                        + "PQ2huvBiCJnx6G7BPWYusDvxZRLI48jMpPrQCF4ZpYTSiVIyaQ9FmsrUwiGh"
                        + "9j+9hOLcxR74wcB8Gfb4obC1OPIYiTF/+A3Fr5O0zbYwWVzCMtvStIDxESPh"
                        + "MVZmm1OmNKQvj5zyDZyf2Dht5pzNG2yfN53a3NmbOrfka0xmaG2mcphsC2XU"
                        + "UZk0q00JZ82M9xwvuJ3NVXWOMvzvT5gO4DLanGvwoI9Q+QP3YBFZXgMAAA=="))
                .issues(WakelockDetector.ISSUE)
                .run()
                .expect(expected);
    }

    public void testTimeout() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "import android.content.Context;\n"
                        + "import android.os.PowerManager;\n"
                        + "\n"
                        + "import static android.os.PowerManager.PARTIAL_WAKE_LOCK;\n"
                        + "\n"
                        + "public abstract class WakelockTest extends Context {\n"
                        + "    public PowerManager.WakeLock createWakelock() {\n"
                        + "        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "        PowerManager.WakeLock wakeLock = manager.newWakeLock(PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "        wakeLock.acquire(); // ERROR\n"
                        + "        return wakeLock;\n"
                        + "    }\n"
                        + "\n"
                        + "    public PowerManager.WakeLock createWakelockWithTimeout(long timeout) {\n"
                        + "        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);\n"
                        + "        PowerManager.WakeLock wakeLock = manager.newWakeLock(PARTIAL_WAKE_LOCK, \"Test\");\n"
                        + "        wakeLock.acquire(timeout); // OK\n"
                        + "        return wakeLock;\n"
                        + "    }\n"
                        + "}\n"))
                .issues(WakelockDetector.TIMEOUT)
                .run()
                .expect(""
                        + "src/test/pkg/WakelockTest.java:11: Warning: Provide a timeout when requesting a wakelock with PowerManager.Wakelock.acquire(long timeout). This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user's battery. [WakelockTimeout]\n"
                        + "        wakeLock.acquire(); // ERROR\n"
                        + "        ~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n")
                .expectFixDiffs(""
                        + "Fix for src/test/pkg/WakelockTest.java line 10: Set timeout to 10 minutes:\n"
                        + "@@ -11 +11\n"
                        + "-         wakeLock.acquire(); // ERROR\n"
                        + "+         wakeLock.acquire(10*60*1000L /*10 minutes*/); // ERROR\n");
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mOnclick = xml("res/layout/onclick.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"nonexistent\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"wrong1\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"wrong2\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"wrong3\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"wrong4\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"wrong5\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"wrong6\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"ok\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"simple_typo\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"my\\u1234method\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"wrong7\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:onClick=\"@string/ok\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "</LinearLayout>\n");
}
