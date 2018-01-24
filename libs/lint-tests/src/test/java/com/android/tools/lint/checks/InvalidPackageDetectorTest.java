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

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Project;
import java.io.File;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("javadoc")
public class InvalidPackageDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new InvalidPackageDetector();
    }

    public void testUnsupportedJavaLibraryCode() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=39109
        //noinspection all // Sample code
        assertEquals(""
                + "libs/unsupported.jar: Error: Invalid package reference in library; not included in Android: java.awt. Referenced from test.pkg.LibraryClass. [InvalidPackage]\n"
                + "libs/unsupported.jar: Error: Invalid package reference in library; not included in Android: javax.swing. Referenced from test.pkg.LibraryClass. [InvalidPackage]\n"
                + "2 errors, 0 warnings\n",

            lintProject(
                    manifest().minSdk(14),
                    mLayout,
                    mThemes,
                    mThemes2,
                    mUnsupported
                ));
    }

    public void testOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                classpath(),
                manifest().minSdk(2),
                base64gzip("bin/classes/foo/bar/ApiCallTest.class", ""
                            + "H4sIAAAAAAAAAJ1U7VLbRhQ9a4yFbVEIIQkfoaWUUNukVkISkmJKYwxJ3Mo4"
                            + "kxCn7Z+OkNf2FlnSrASp+1RtpkNn+qMP0HfpK3RyVxaDx4HQqT2zn/eee+7d"
                            + "e/T3v3/+BWANdQ0JhumQB6HhH7aNsi8qluPs015Dkm4styk90TQs3zfKdiiO"
                            + "RdhjSG0KV4RbDCO5fIMhWfGaPIMRpHWMIsUwYQqX7x11D7jctw4czjBlerbl"
                            + "NCwp1D4+TIYdETBcN8+LX6IwXR52vCbDw5x5SuSNaLZ5aFQ60nM9uueyZHqy"
                            + "bby5ZxtNr2vs1GsqVOjJUr6RAcMVHVO4yqCTm8rAc7ctyTCby5vDyUVXJQ3X"
                            + "GOaHMHel9OQzcnC4zOAGZjXMMMxdzErHHOYZbtJx3R043xf2oSmCkLucWDwa"
                            + "YPE+xvKFniXFYUHHx/iE4WrAw33+c1gNXnKH22Fc3NwP+UYan+IzDUtEtWk5"
                            + "x+LQOOiF3KbnMuq+mqpuy9OxjFsM47Xyd9Xaq9qPjbL5apeBVVX5PteRQ54h"
                            + "TdxecN+TIcPKcO183xFUcypgVKe+XSmNVXyh4TbD0uXWOoowGLIHVki59hQv"
                            + "hjv/Iczy9pkHhbyLexrWGBZPPdvS8jvCDoznZMzlzlGrtVyjzHXcxwMGrd7Y"
                            + "fWGWvyeS5mUu1JJZ++xB6HU/1JaE7fQ7kWHmoh5lGOUqF6WC81qODEZs5xdq"
                            + "Y/Mn69gyHMttGxXHCgLl2hLcIXlkWsJxnluSu/Q42a4V2p3TXbJLvCPwQfn1"
                            + "/FMJTg/Dbha2CHmcknkqRdO0et4RwSy836dn12SfeekdSZs/EQpzckDFRQVP"
                            + "4qu61LVRAB5o+IYE9oFiE21Vbg01hvv/Rx4k7wvvSBFUU6hfklb0vaJRo51B"
                            + "M6N5tPAHxn6jRQIZGlPR4RVkadT7BjSP08zwESZi539ia6NwgsnXbG9qeiO5"
                            + "eoLrr1eTJ7j5Fovro4n1VGJdI/CV31HYGHuLOxtpFWYkcixGAaYJ/AYBztBu"
                            + "HpOk72vEdw5LWCCRLmIFefoXUYisW/2QMR21WsfDiKKBR/iSKGkk3g2UKEaW"
                            + "/DfxFeU8R5hbuEWRFujL+DWtUoQ8gce00gg9pZSIMRoZyniANLbj1PsYFcIA"
                            + "drAbp347rluC/TpUtOJA0RIxy4T6nFzquXau55NofIpnNE/R6i6q+PbxLEyq"
                            + "2V4q/Q6UwMkB1gYAAA=="),
                base64gzip("libs/GetterTest.jar", ""
                            + "H4sIAAAAAAAAAAvwZmYRYeAAQoFoOwcGJMDJwMLg6xriqOvp56b/7xQDAzND"
                            + "gDc7B0iKCaokAKdmESCGa/Z19PN0cw0O0fN1++x75rSPt67eRV5vXa1zZ85v"
                            + "DjK4YvzgaZGel6+Op+/F0lUsnDNeSh6RjtTKsBATebJEq+KZ6uvMT0UfixjB"
                            + "tk83XmlkAzTbBmo7F8Q6NNtZgbgktbhEH7cSPpiSpMqS1OT8lFR9hGfQ1cph"
                            + "qHVPLSlJLQoBiukl5yQWF5cGxeYLOYrYMuf8LODq2LJty62krYdWLV16wak7"
                            + "d5EnL+dVdp/KuIKja3WzE7K/5P+wrglYbPrxYLhw/ZSP9xJ3q26onbmz+L3t"
                            + "83mWxvX///7iXdDx14CJqbjPsoDrbX/fzY3xM1vTlz2e8Xf6FG5llQk2Zvek"
                            + "W4UXX9fdkyE/W9bdwdp2w1texsDyx4scVhXevF7yK2z97tNH1d3mS21lNJ3K"
                            + "siwr7HzRN5amnX8mOrzQPNut2NFyxNSj0eXwq5nnz/vdNrmfMX+GT3Z5z2Tl"
                            + "xfkfb/q2zTG/5qBweYeXRS9fuW/6iklpVxcL7NBcmHhq9YRnJXr2K2dFi6sc"
                            + "6pgQl31A/MGV3M4XHFXGTWsYni6f3XexsjpjT/HWnV+Fkt95HnEzSA2at/r5"
                            + "SZOPD5tmh5x5oua6Yhnj/Sl5wsqrTDtN0iyips84bOPu2rk0MWRShGTYdpWw"
                            + "wvmLu44opSndUGSPu222PEuo8gXTxmW1197PYBfj9ou5te2Y1YSl5xRq+wWY"
                            + "ciRcGcuc3waW9n3cmvHc+tLujdwlWhf8pjlcrlf6F7pVPXNu0EmFdZe12nk9"
                            + "HrLdsNl1ieWHdZp9f2PyvoSig+xzfhqx9f1uEq9Vvy81f84nVv3Kyfwro79+"
                            + "fGLf8WrlU/kTMSc4tJbtKCqeZ3NGIK2wxfCp0b3AvUmzJmnPW2caHv5C+l3f"
                            + "6VN9E1psIr980NvmVP2A682qQ+f4XutNWzxnFfc/RT3vq6kfayezK5vMcl8c"
                            + "aLcoQ67q/6PJrwN97Y8vFtNljTOruJnz0vPWKZn87V9Cvsrs1t2/7fT7EJW4"
                            + "OhPe11/0zSYs8JGaHeHAeVpjMmu0SfVsLdGuVTeOnuuIND2/5nhX4Xt7UEY4"
                            + "ZPg5Pw+YD7lZQRmBkUmEATUjwrIoKBejApQ8ja4VOX+JoGizxZGjQSZwMeDO"
                            + "hwiwG5ErcWvhQ9FyD0suRTgYpBc5HORQ9HIxEsq1Ad6sbBBnsjJYAFUfYQbx"
                            + "AFJZ3LASBQAA"),
                base64gzip("libs/classes.jar", ""
                            + "H4sIAAAAAAAAAJVWCTSUbRseBt+MyZdlsjRjzxChH5F9KbLNGFsllCVpwpRl"
                            + "aCxZJzPCZEu2JorEEGkxjRT5KJQt+5aMlAhFi6X5R8sXzuE///Oe95z3nPe+"
                            + "r/t+nvu67+dCWwA5oAAQ63mGQRgCVi0wgAOANLYzVDRDmSh/bwQAgAC0xV+g"
                            + "lV/sv0zQGzpDWe+/zkhDlJmJsa2dEtLkE7K5ydJCUamNx0JR/kVzS4XN7peq"
                            + "r8b8lMyRu8yQbbgiDvDldyJ1MJjCyb2CUMZ1+bNvZN5jPvrN+rH9iG4Q+OKL"
                            + "Dgtb51d07h/RMtZF52S9AR7+Acobm4B/m5zx8lT+s4/1ZmKrzayw+7wx7l6G"
                            + "7gGYQEwAXsnd29Xfn5A8iOU3hDYwPXNDFRRdbt6VvyuPTsZS7yHc1JACKYis"
                            + "YOvo9BMQmNbtSewlQiPs6+6hy5dPVs2whUKO1Z090evMl3uxOVtTnDmsqdrU"
                            + "MjwcDug5IA0mlI2IAK1wwc7zd3HecYHThVOkpJl6szqwzOBE5b23hQ9GC13P"
                            + "XzyrtGvkoUTdqXlByXPeQfFb3EzHy6I4JG+Bm/aKfm7h+ZJwDrs9JurEfVxF"
                            + "Pt1owXD3B2cim6uto9q+FEfnB/c6w6Binx3stZOQZjNa4FEyeCxTEKXNX5/f"
                            + "97rkXXp9vpXci2exC7Ix8/iF4JcjLtbGCtS9O+QCyRenyOFhS+0aVRD6kyGD"
                            + "OdGOs+MJPrLU0MxadSumJEG0dt7srQNB/KmaloiOwR5SbExnWW+rQEFR7yC8"
                            + "Cy1t59UXEna9uACL61Rt2Xbw/rfw/agrmhWx2P6zMgyv9BNiQVbmZTzf4y4+"
                            + "2kX5+n72ynErDwvvQ4GPpOYztnKrVMkuucRdiBy4HjG1515okunATNjRitrJ"
                            + "1ItSrmLFkTOPb1GkmxdZCajwNnZs5/5+1TiVvc7iaDmemEcutEy2JcLi80EF"
                            + "ZYru0hGit9MU03puiBzBn5KQydvJN4VQTx/EeNXkIIhqWGMiYdrh27huPrqv"
                            + "ZOxhUOkksvIh3No5qrIC3lbJzzaQwHvASbq4nWA+MqQxEL4olDYrELJ0w+7G"
                            + "mXeg9ERE0WLmoP4KW5NDozuFWc3Czrm6V9ZzDLaaY3asD7Tf6UDMcQ+/nwRL"
                            + "s3H2EjCE6moMmzn23ynoXGiz88HEVUpImV6XYzMEy6txWfMieiVRCXTfQPVW"
                            + "JiNaD6z3Fbhk/XqwFynXa/9erzknJ6Qlw78lh/Komgk4I7TowQDxSANHLfXi"
                            + "s8VOHz0VXuR0s0mONzBm/NQx2ovWL/Fz8R2kUSoRHVyo45EI+hZ9kIJ++7WU"
                            + "KkmeUImT4ztIb0xpeUIG3gxx230pZr5UHzpa3Sq/YPU35M1DbpDjoKWiT3VD"
                            + "mjum8mONYRCiMaUen29PD5EqqBWBnGbAYSVV1os+C4XzqXvOH+Gb2MlutMdl"
                            + "Ide6WghsGQlW+BTVEFxnrqFSKpF6oH0OJbvLX1mxIo0gt1P5Yh+vlkjYcs1y"
                            + "tXCsikt1gHj+zeezjO9sCLvQW9fyiTpjvQ8lofoEGwp/AUZG5PmVprn4/zTT"
                            + "nF3rDDja38VozVwT6Ub06Wcgcxs93xASxR32+pa0djN92mnDJlM9W+fRetZt"
                            + "/lRKWFhQXvBkVJjRXBq+4XRWtwpTE05yn52ByR/Dp+xL6E7WpbqQuscqI0a5"
                            + "D9RyUhGvPOEQPgQTz0Y7nhTrq25oQt/z0kcvFuc9s31KKa+mHZTVY3522v+O"
                            + "5sLtx/QTeA0BhpTyCL+SLJycfL4DztPUFl+R0XfPl3Co1t7Jogcrq56vPag3"
                            + "ao390Iab3PE0CG6RoZUpiId6quLUHLywHw6e+3BwckKJso32kQuLCJqfPFlh"
                            + "96Gf/uGf3kdz2TihkUvL/Ys8K8RrSbW5sptFPPymxINvRDyVn8zzQTX9XYPm"
                            + "1b3WadNiSiwRFczs4YeR7KX53cIdNZxuNdp/bzJ3GwGGpgqnJ9RZ+p14kY3X"
                            + "vXJfO0KpUckm5ogUmRTes2tZRFZAlNJpNFEKyUIvF/NlV/cVHzcoyyzRrBGr"
                            + "h7yrMVInq8GNdqJDNPWmjG56Sg2YG8kQpctzICVDS453UtK0+CdqmELBHKe3"
                            + "UwJ2HGic/Gqt7eUreDKC8rifmENWfdAc+iQyYDGumZAC1TFpgCoyhD9RuBI5"
                            + "VY+qOecLrRxFe9IEsIa1UXG2/6sHbTzcPTCBv3sQZ91k/oR1Eh2HvKldRSVd"
                            + "QoLQI0Jc4vS4KM5ctFMh1GkIIfSwD5F83ZhpfigqSvzVAVJKf0T+XYfmjO6M"
                            + "7IzF5U+znK94Hbn4Y9zkoluDg6sqsD205Ht2wHcwUz75WlyolealKVpuwL73"
                            + "A19i3C88Ujd6s58mryOzo/iuumTnDBwjRfL+G3Mc9O2S35jHo+6k7wlvA2Rc"
                            + "y8d68/oFq+Ne783gnrY3OIxZLi5APQ3GwvkSi0t8PRJAlCcL+3Nv5ophVCdd"
                            + "BXJ10logO3MkqagsY1Pt5aPThDLHEqQacqAQ60LQWAwqzwEKyKRecyzh6yqy"
                            + "7SK/zOo+Onx/jBD3wl5YcVqfGCnByCxVWDY5xBG5Bc2mD85Tfiamz+kYLX0h"
                            + "SbgjZPoy3l2WLcXHRJan+fZzHpjwuDD409MGhqpuov9onQNyQEzQ3Y2hePXq"
                            + "+6WJw24m7E1y9U8S76hILJLaFYsdwkRhmeLWKQmxiLLSKS51EWRp2tytGchK"
                            + "+YwY8NRAVum62Dcr3/b15bP18AvEuHv8qh65HMVuyHueGRndAeSM5HXULnMR"
                            + "gMDiCQjEDiEYaNnvfpv9gEzQXy2ENBJh66t9Ip7pHcLXHJv1K5ihGRlfyN0a"
                            + "AM+3hELeYEIjpDy4SkAVVSDf/3Y/AQRyxrQ3T9g97TRP5co9l5ugpATurDmD"
                            + "ApX6FEnZlQ/v58EZKXSZDJZSBOWznv2TTEJ5C9CymyzpD5a2xIXkCCzMHhtt"
                            + "0KnPVPCxqT3JGz6VdNhWzpZISp+d6S+jXa7EpmnahGSdWRI3JRYE5shMmNDt"
                            + "+/QRZDGGyWuOULGB4jP1nr5baPzUuiPaYTVDFfelE3FMkbyacpqXi66LDDzP"
                            + "vnacuq2h9bNQ78zx8PFL15v69n4EaldPnGNfOVh65eAEH+tgaT/6go0dClir"
                            + "5H5rvBUZuHatEYXrXVcLNOgaN90NJOEKAjdgYyH3Z9EB/8q6jV3Aa1yGAGtk"
                            + "3p9cV9xWX89ia9w42f6X7FuPtXriwtZgYYCbXu/rgVbPK/gaoNecm4/r9Uir"
                            + "W2dtSlNcm0679UCrqbJ9DRAVtFnfoS04uX6WBAw4xEqC80fF/gu8+KimcgwA"
                            + "AA==")
            ));
    }

    public void testLibraryInJavax() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                manifest().minSdk(14),
                mLayout,
                mThemes,
                mThemes2,
                base64gzip("libs/javax.jar", ""
                            + "H4sIAAAAAAAAAAvwZmYRYeAAwj7P2c4MSICTgYXB1zXEUdfTz03/3ykGBmaG"
                            + "AG92DpAUE1RJAE7NIkAM1+zr6Ofp5hocoufr9tn3zGkfb129i7zeulrnzpzf"
                            + "HGRwxfjB0yI9L18dT9+LpatYOGe8lDwye6ZWhoWYyJMlWhXPVF9nfir6WMQI"
                            + "tr3e3eK4DdBsG6jtXGDbWNFsZwPirMSyxAp9hBp0F/LD1ZRnJJaklqUW6eP2"
                            + "jhym4nAow1AvOSexuLh0wlmvwwECrftnH1nSu7HTnrvwuV+3xK21l3ofrHrh"
                            + "FBhz70j3vaB8xWVb1z914uKqZZD3muMRO63x8Mz5xW0z388/r3ohXV9NpfdE"
                            + "ks0lM+kc9zcub7Siru29dmuqW6GBzMuZ857nn99lZhz89Lv2JfvglufXblYt"
                            + "+NDf917k4NEngSovL1gVm9Zq+pSu7t4Y8mj2yS1aZ5fmTjq33/n9lo1+GcLa"
                            + "366v9ezIWPv+u0jk5VPHb9k+vX70xBTT9iIfDXctzsD1M94I3pnyhiVeIuUu"
                            + "66rfrd83HPivUPF/tg/L7PkKwY0tSd473J+Lg0K/N1h6/0NgUFgzIsd9D1pg"
                            + "yeILLJCEtofPOT8f/9ATXjreay5on/fQ8dT11t307JGWhu+pkz5hHuc8dfTC"
                            + "ONq7pLVCigudUhOS3LhmVh5V7aw8uZJrG1flyqccnLy+2eui9MBpQvTrm4/e"
                            + "QDuT8KZIPFFoBIlC66/51+3Y2NiU2eL02NLY9MrY8srY5NzYzGaf2XNGyNqN"
                            + "LY33rIEbm9jzP1NC3Njq5p/ZE7L/0p0lVxZNOLJEZgFHeEFdZmDRqmi1jsPi"
                            + "8yZGTIqaFpWyZPrUiGknDh84dODUCQXGpPAX052ffnnYJSHJ4hD0y7Kz8uWv"
                            + "f56RK0+q8nTx+DikNzAoCzB9ZfY0U/AXO5OekHD11rkDOwR1zvKp3DzQdMZh"
                            + "ErOHsWEBE9jPDypWZqwA+ug8ip+biI8JIxJjQjOk+Co0D+5cuu+DIdBkE7Dd"
                            + "jEwiDKghDisdQAUIKkApTtC1ImdtERRttjgKE5AJXAy4iwAE2I1UICD0YCsS"
                            + "EOA+ZuChOxk5P8ihaOZhJFRgIJsFykHIqVgWxSxLJgL5Cd1ZyIkD1VnncRtl"
                            + "hOksUHJCjmxUZx1mJpC4ArxZwbEAMkoGaLE9ODkAACKr4XJxBgAA")
            ));
    }

    public void testAnnotationProcessors1() throws Exception {
        // See https://code.google.com/p/android/issues/detail?id=64014
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                    manifest().minSdk(14),
                    mLayout,
                    mThemes,
                    mThemes2,
                    // Just a subset of butterknife (just the InjectViewProcessor)
                    base64gzip("libs/butterknife-2.0.1.jar", ""
                            + "H4sIAAAAAAAAAJWY5z8bANu2VZGqVbV3tVbtmBGrdlGrNrFnitjELKX2rNp7"
                            + "7xlib4nam1KExp5Fi6rx3ven93k+Psf14TrP3/W7/oFDW/0xHiXOkydPcEzx"
                            + "z+Vx/gf8OHg41t5eXnYeTjCovZ0AFPafCLNyFlCFfbCz8TKA2vlqe7ja2Hl6"
                            + "unrw2zhbeXrej+LgtJj6WeotLDzkiqd0jYyrPyF8RND66LFt4ssCjqihJMck"
                            + "9ufEuN6ZtuZfkkfe5WTiNXoPrp0gW/A7jWrLkM3eJIoHBtXNyAVkXa1J5zJq"
                            + "8SRW/4X0bo+4qRBhsUWv7Pl60FVewIlEr+eJxO56aDmp9LVgiJJJKMNpuZTf"
                            + "VsBbTsNHk8W40PbS7eQ01lp5Gio/R3qqAMGwMnWt6GyWUuYsOTvmLFWElEh2"
                            + "13lG8Rw589fpZDAT+2mxNxgdn7k1B62L3JlpZOYu/zuL6NMgqpRnTj2vmK1c"
                            + "URIFL3S4Ey8QPvjnJngW55x7gdfj8c/NtwopHBsBCGi0VFmA4+vcvaIUxYaU"
                            + "0vLCEHpo5jevmOa0zj09OlOdNxpeWbFZ28X9Sjyfkaj49nnu2jnpigJhqbYo"
                            + "E7tNsWTX1F8yuqDaRWi8ppNZ86TSMT94TOlQ2rGMtBCj/cZBtzxWTG1KgnlF"
                            + "MXu7EaV0mnLVDs7d7TcWWVPaJVKKBZTP4MPOddNjO+wyQ2LfGSNUys3FkVES"
                            + "SO18TTkaM64JA9YV7reR4TUWzFb7sW8xIdn8RRVS1udRsf5hL2eTNH9IPZlB"
                            + "yfeuKaXD9+5E1MvJogzSO/aFOiBH8M/fr0dxV+QMR26rpb1IzHjKeiYVluZG"
                            + "wGtvvp2COltajhnlRw6I7hTub5JkV00x5IkEg93x4QM7b0HUV5HY3PvpAlXB"
                            + "8aRl1wisw3BYIW1/+Q7EWeMgEW9c++5F4nyrkR5wR/sZRsZyZUtFQ74nVDVc"
                            + "rJ6TVqg43MYF/BHEGP0P2/+2LfgF/b6YDTCtqkc/KmNFlFKkdURvoCs6zs4Y"
                            + "BdcQpI9/EnTfZAmitdB009PzeZ4z8U4lAwmmFVsivfTNnCG5r0t6/Uax465Q"
                            + "86MIF8anXHidxpakysCXBI2193ICfbdZBAyuEv9T+OOySTRxAuccm4ONVFJ7"
                            + "jfqHl1LpVs3lOSPDIz/NvDVKcqIkZLKHR7kNBGRoTLvQDzx18iPaJxVeB7H3"
                            + "/knud3uhkDTB++2zNPlA8uUQnO15zEL9T6SRtVl6Bf8vWCwB1rrb2qnbsiS4"
                            + "+GATaI039mXN5dCE/0QMYAtlvogqdY5k5c1wVP2x/RXoAhadYDkWxU6w9nJJ"
                            + "CHSaiZU4DnXD7b0ke01rWmeoSbN1RD+gv+By5yxeA0apqLTmPm2H00HDXMbX"
                            + "WAw5fJYHUEPJQlYc892SJYRKUHAjXqZUe5iqvT2YU8Facu/vcolFGkNWJMBV"
                            + "QiDofdmnsSpPG35dcbunV7JU1Q2vTk7XRyFUz8BrFXMAib6D36y5MuN/Y3Sp"
                            + "HF5PyjLGgj91W30MFByF4/OsV4NPgx++XuINhpa5FayxxGem3R4Jlmsy0XB4"
                            + "ZG0Qo+zbJCl1smUbPz41OrfUmo47x51s6MVQPpD8InVOoS37ZAcMcB0nNvIw"
                            + "OtMVp7hAKM7iPa3sFnf5dtm71aSfonU4XpLatpeo7KcONVUsS6LbufS6sEYE"
                            + "COb1I+yf6qB1hQ2vQmRHb9CpByioUS0p2naVnPhXSPGYgGODzu8MaMSPElf6"
                            + "KVvm0VITgQPSWBD2hXUVCntr57G/+J1D+8RvFKRado440x65PXceM0n5AVf/"
                            + "vTKW42a0j+Yc2o6HNLtvFs+peN1wGQqBLylg170S2w9eDlNb9d4zOxX7idkM"
                            + "s8V3ZTHupqyt+jyeqs9P4Cs9gh2lrbnY2N50WaL5XZOWalj99ce5248V8cmO"
                            + "pvL1dd90RglLLe2QB9PClqw2QN8fFtUa63JKz89XFnqakHo/Ok3AmUqIumpT"
                            + "BjE0vkDhgzRwEhpma9ZVIvk84V/YplHM1+mME5KJZO0op5evXUl7Y7Yib4Oz"
                            + "42Gi1l92EefYn9tFcrox6RE1eBKVKJS8j7tnVm62mrlPV0J/aOhbERgB+GXY"
                            + "DAyR8kpYYECirMjjZQoxvX6NcVJN1kBXSudhWjN/p3xqjMnpLqgMKbRbYB7R"
                            + "LkLEKilAhMk7WSGRuDEyiHQ3rVJtfJeRu12hzZlC2/QnbBPegDkiRn/Kwcv6"
                            + "8ihjGwK209PEzc1KkXpPYgxozYT2F/GzP36VJNPAv8r41MdV1I2pTvwXuZvJ"
                            + "dYYcS3k2MSSBh/gGL2i2L/W71XT5nv1jw80Pk6n3bi/oCQkOtkAGnO+8b5rl"
                            + "8gtzCY6LTIkhYU4kcT/l+CeIb3iDEniEw2S0LDbAEwWm+7EEa8qtiBfGB4Zh"
                            + "PWwK2+0hrtNM8oeSED2RPU16+45Bv1RNC7E9/psYTQtRR/GgiYL6uQZNjOAe"
                            + "rGO4IICOtYNsiRNvSmrpGfckqTr3FeqVjLfgOT7VXnJFfeSD7dj1zjD9dWJs"
                            + "6hm/4/SdiPMGBh4ryNMd+MRV9LHDtU3QgVuSd7iCaVnj3GQZhIzhN048bXPk"
                            + "5YHIh+mw5kS32xjK6MTDNO0XO4cscaigHZS7MOOvP4qv7mREjYCyBuKyBsYO"
                            + "0+L7rGHM1KYy1tPFvzPkMEXDJDdi0sogP6Hgo+we/jnxZySk7spXQGKg++KJ"
                            + "SI/WdAr4WHxP8rfZwChhXODrB4e8++Oet6BmMziaYBxW2Y5LjKj/AZ4h/DEK"
                            + "Xpdc3tpsR4KS3d4XxUUTSLOrBSfyyWHKWWhLZuYIgiOWiP0jgh2nE/eGCaTV"
                            + "Ex1ghf4BRO6qOQF/RtebBvLSywrxs9gLMcOpLhZsfQc5GWwtHR746W4rbZmn"
                            + "hBMoivQp/LFd0PMG7/ylffww85L0a1CmWaLMQ9LIAT/jz6SmaRrRv83USKT6"
                            + "qHqwvOz2l4WCdHZ4l7wWxeQod+qsCjAunC5r6G9fa63A97VdIrRzPXUNQefX"
                            + "vlRodZearM3glkmNlh6XS/+gC2e8+dz+LsHdp+CRFkUxi6QCK0QuywW72jML"
                            + "S7eQ1/inyFSpvppgpd6dub6sfm0pUN+mAy7/oxSq+Cbo5+IsNuW0ymum4Pwo"
                            + "bijdUXO/JZelMol/bvAgfMv/AWXc3AXxXzlqc5h2ZYf1qPUKu+EASG7p6z2L"
                            + "CjEMvwnuQrVYCe6SfWm7VT8K7U39pgm755a5ED9L9Vt2SkhF8n/BJ76RPBb9"
                            + "xXE/NNwJzr1P5htKrNYjvuE8ntu3ILhjuIUTsYjvyf5GagKIwYo2EQTxXhrC"
                            + "IpE7Is1SfBZLXvLHepVtlTzUiTedWCiI0p5NjP8F5zqb3bPKZ+YXME0rutdD"
                            + "TfSD+X4nW6CHulPyv47jxgh27Ki405nY/exSg5b2ZY4NR24Kly5EqnCh4zY3"
                            + "ny3v5uZqr7K0oaqt+eaFN0VVmcjV32ImDpq5TRgCGAtfdmW6uAMe2hrlaZRG"
                            + "+07ye29X8IK7izxWlyMiw1QBGZnAHB9UxdVfGycJn4qAwwmP6mIZez09ex6z"
                            + "C+N94Ylj6qLL0m7+jSoFo50S28/gqwY4ItK72hPuz9cJfS/Vythjr9UnPuJy"
                            + "njOi0l6UQJyxGX/p7akqX/6byMHQcH4VqZa6oJQZBnle7R+nbQAkJ5Jg2/oT"
                            + "TmATdJvWKSqxJBTo9Hw3YrmShBxR7SLcAKQ9TclNJo0Eh2g1wyvH892c0hey"
                            + "OdY7mYAojpeUeDZMYWaLTJh9Y1JebLdwaxgESBS4WCBbcvDoUETElxv6eSgB"
                            + "x+m9YbInWYAxGrbS01OLgCqoH1zlV54sfkdH84Imv0Zf5DHWSIqJuDg6IZF9"
                            + "mf1uIRRGpNpEdli94QQR8GXKzoIGE5PuvVfySKv2JLo/WHOKOq8OzkbebMfl"
                            + "HYX94PQ+/700VFJMtFpHKUZs55AMz6R0nar2Bwc4wOdqaftE1yy5dWdDUUJJ"
                            + "DkBkvY80X/C8TPTRjdBmqI7WkQn7NGi124FMNf4YzThVpZ17oOs7xE5HvtTD"
                            + "vhjWmHYfCgJoO/DCCdX391I3zAxm0kRXwTzc29QBN+HqxjslmuuH/mzNDu80"
                            + "IT/OE+wgixUdvL5E0Yb/AKD7s5wVZ3Jn+XSzvjP/OZunjy6HFHXQ5tkuh7a6"
                            + "f7Z4Y7cPdBNC5V4nIXhTsT//sgqv8sghQ3j+ZHQbo+nYvaVpRQHy97D2PxWO"
                            + "sEm1945mWpkzsEPl5XaftRVq4FteuhQH205WrHOfTprZN4/N6HWJkyonk6gL"
                            + "BV5vCjQPWYmhCZ4Wd6fiXLHupW1OgfO8qKEQi5Gqh900hiFh6PqbIeyUWFKq"
                            + "pcaiFxZ0MHxVM48LZHlyc3d2FDHpk59GsnQOCd896isfcuOuhNVlvEeOVMUx"
                            + "xsRp8lAsp/s10S1tueWDlCIaJEo9zgcYVZfpEIdR7SCyU0ASQEAmRF2uLa5X"
                            + "JGfqYzr26ULPc8n5f9yQ2vWlDZPggIxLMGRqiuG6Og/DFlh1GXKiYyt+Lmzh"
                            + "bbARdivCixv4hGXnCtK+ieC0eASYe8ypr38aaqAv/RSw+QhUmspZGwjdn/XQ"
                            + "118jc1Dt51qlV95MlnnVss5oTm40VM2LFuG1Kk37aZQTTuHe9MWqrCWBL6xZ"
                            + "JiKRZ8Z1gBMeV1o4XDg4YdNZ9eGywM3lmCsmKj1L2dHNP7eYi+NoyG42SxNO"
                            + "yU7it96l81UtQFmhp3msRBoh56BjZh6Eh8ghs7rkiBTvtaE0B9BDzJe4dgR7"
                            + "cV1D41iAn151478CqTGrkGH3CAxTxXuH0q+e7VRBs3PMl57seMdtfN9sHMvp"
                            + "NF7ncB5kBxFolvo+jrluj2uxSFDLGc2qt4xJ/R5H7VVMdQdi0snp95zcMnZx"
                            + "PtC/pjTAChtVu9FZesLQOtdsVx1wNPFqnFZxyH7XO0TKv7QN3KMBvi55CrEN"
                            + "xDVjsV70M5lEAaGV0iPK1k2ftAh39aJOIosBptGDTtpdSTpMoLkVrYV9AAbV"
                            + "LRNsKtUzIMV7Uy/UrdSSrdKL7ubs83tWCu6pwzLY7IWqD6+BT2bc6xfKsTJM"
                            + "V4dHT0pstvjG/bJpVooh1gJDqszNxIdVNskOOJOjQVyYZsDF0tEgbrAAxNTs"
                            + "PTBGzaUreX+oJNnBVLmuKX48Qj+ijsNtjlYpPuxzvun2BE+lBq0LpTdeIyTP"
                            + "DrkmVJj399D8zpxRPAYrmqfLyxI7P6wZ20+YAvzObRZLeGU9wpTBXqKeSwjQ"
                            + "wasO31BkBU3aV1RojpZw00hocsQVhDIBtfRc5434ggVe8fI1/KFHveat7qxC"
                            + "Z/cCxhikzLWMQ5e2vRM39EqDelYCi40KC/sr/ggRaNCLkbL4F3VsNTtbfmAQ"
                            + "7g8knujUfnle69UQqNZM0+WXWLS8oiA2748rui68X9GOLly0Li5LVHGJ9LIr"
                            + "WPyALTTFC2bNiFrZz9SLL/pFV+SWnVTXMWhyOT6S9ueZKyyvyeTN7w6Vxgij"
                            + "E7K3TnwOwwJGc0saQnfTV92wYa4VFL+DKJxyI0+nJn8PxpTi3jaTZ7msM9LU"
                            + "L9QRWZ5rz5CLcbho6LdfRI1/TihVuqxzzNgRpY/pRpEJ5J4Ymbw3eC92J/Y7"
                            + "IHZM7SAtzb88NfVEqbzpL/84hZzYyqetRuIP2kNu/Hz06JeuVsaVS68aj3PM"
                            + "uGh+alA3Kg0L/Zg1Fa7jL9xszB6N8Xn58XoDq+YrPbEc49SNQiKYuoo5nNxg"
                            + "6mST+pV7kE5A9nLneb6iH/e700Fqg6ug9VvyHFMPGVllt/4nt4M2VwurE3KK"
                            + "KMOOOIqXDXlYS0CMu5jKoGZCWLYbqYoGhSkoGKZMfDr4TGVkbNOBrgO07o1l"
                            + "DsYk4kQWywQyyEkl1sIJybv6hdf7lUDhch42UQkDYlVoH5M49T09iJrDptBK"
                            + "3Ng+BKIq2RAGG9L3Eq4ry21LgKH+NDOwlAB+sfoTsJQZ/iK/4evF9KnsawR+"
                            + "WkdzipLBFmMa1C82qY/hgQamcaYtBoE6h30uK363l1gfvhDJFnNip8fXTT6E"
                            + "PUVmYfUfxDedVnavg3h66tYSApo73euRLFfQqc3gY9Hgh+dkjvU29Ze+gboM"
                            + "gwoJz6V+gnATJRLQrYxEZ23FhBifZiyhCk2e1+9fklF3CU96sG2UGO/V8HLG"
                            + "/M0rvus87MW7f+xSx21EUyAfrPHwIHyykWVKMm9APUH8jGKY9XZ+v29s0qPr"
                            + "ABK1xKaiw7lTGN31vbxpehtOh9k6KhIOPoZHqdPQFMOfMqNfcVnyHi4j0wcm"
                            + "ovi1l0cHM6g3gPxmxLyFf6+pmXdzPIn5Ws+pnVbGqf1xmRc3IajS+X7q/pTv"
                            + "idbD7lFYyVXxNXdc02MxWOp07Yp8II0Wa76PkwpXpZf/Gx6d9+3N5h1vVLUL"
                            + "SKaVA8kwu5/s4vyiSpjRx49uQyQY4tZh7BasNnCaPLQwlzWbK4osc0wEjfrS"
                            + "nuHjyJc2TmlNxJ1JV4p/TX4x5jCu2SzUiFVSHpwcmhxV5NhJ27KQSZXcrbqt"
                            + "Lvy+j0r0I+GhyO+vnJH/vVxCPdVEnXY01e8y9ltqzLaWne7HT6TkyDG+pI1s"
                            + "qESkW9zoZE5Zr/uzXLD5FmMQzkXbLhfmFlgHzuWON5XIIWp7zGU9+7jCalbB"
                            + "qAjLnjr/ueP+RdrhdXRCPvOblIR0jPIm7RKodJ9Yj+mXtUeaUl1evZvdm6pD"
                            + "qpfG5cm+b9dMv80Nt21kvvjcn7Pef6BHcsb9cBdnpr3w7JqmuvYiQEE0Mo+f"
                            + "sFB4afFExSk+xvK5ea6+6Ks92DcEVi1e3FUAF9pYyvqRxR52vuN30T0pLbhk"
                            + "sOT2eRnbh16xbbNS8T27WIJ87Z60O3Jtq7njoxnvVXgFSBC7DX08k13lTIGY"
                            + "2Hx5uXuEp1kGYu7dMGCemxgQQAEVZ20lkHKZeXrqq/o29y6jf3k5BY+AwNev"
                            + "FTreABrM1XiIYi3k7jys0493z6QAvYk1zHL1F590q57TMkp86vnZ8fOcHO9U"
                            + "M20Hpu//kMp+n/1lzWHCDs9VZ5ZpxH/0r4IKndO/J7pO58VJ8f/G3vGxW0uy"
                            + "4PGzvdWve362KOmnsmr2j0u+eM9QLthbn2T476/JxvSqKtSzScUQ3xjyl7cV"
                            + "l2C+lAbR9xkstUc3LME9E9tHkrwoOeQgq54NcSwtbbIfcKwh6vXH4l/20C/f"
                            + "kdU/M/QOSdYN6C+gNu51E30GaPMNYD8yBAMQVHDwNNm4T35YH8p3/pzfTGXO"
                            + "QdigXNojWvMvpLRTzfPiwzGvL7lkbokq55wSk9SX3VVpqUshlcVwjwPIUGef"
                            + "Ar5RH7knoV/bzmvFx1TsVKYh+9fCREeKhPnf2SJA85kvC38K3X6eUKn6I142"
                            + "N0uZ0tj7Q9Omzx2HvSt+VDd7j0XqUmtdKg07vxypJKbAu/3ioOYtO9RBqbz7"
                            + "cuG8nvyo/dEJ1Z4DAn2mo5l/hKcg2hLQGD/T/j163lhbgnFiiDerFG5gfPHl"
                            + "Ztzg7KveoayztkbLuv2cZMY5TjbG5MxrO+ZNZY6UX/Lx+x/mRYxBXb88Z0kz"
                            + "S0c+q6UmUiCkS/aGbTUIskHM5FDTAtQ38lecKkaCl/S7aXTX8j23S9G+extP"
                            + "Tz8+0lYHPMm7DKkposLB0RfGwdFWf4RLifP/HfD/vP3XAf9v/u9GWFsdn+C/"
                            + "n4/+M5b/2bjU/23/D9p9Tw94FgAA")

            ));
    }

    public void testAnnotationProcessors2() throws Exception {
        // See https://code.google.com/p/android/issues/detail?id=64014
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                manifest().minSdk(14),
                mLayout,
                mThemes,
                mThemes2,
                // Just a subset of dagger
                base64gzip("libs/dagger-compiler-1.2.1.jar", ""
                            + "H4sIAAAAAAAAAJ1XeTQU/voejKyTfRmjFK12IsuQlLHPkCXDWBpjMrbBjG2Q"
                            + "sk2+smQbFZI9a2GSPTHZxSSyl11E1kyhbt3fcup7zr3n3Pu+5/PHZ3ne53z+"
                            + "ed73MTdhAQoC2H8mj1+OHuC34AAAAXCYla6cEUJf4Xs3AMACMDdhY/91xfy/"
                            + "T8z/JVjw5/p/MFwXYaQPs7SSh+tvwXt7TE3k5OkgEznp17391RaKb899mCfI"
                            + "G8NljeB0/xIgx72PYFrGfWmcurDgXL500MLJFbdNwgaB6Z/sNw3UO7R+1tb6"
                            + "g327xPwPdrWfywXt6oolKLjh/bAEPNpTAePtgnXF4hUMCGgfnO7PExLRjQgj"
                            + "ELwJhmi8iyeWII/xRBOJWVc9bCAw0bBTQOFkVmkru8cwTwykFctV/jwi9fxA"
                            + "BMicA5YjfbhLBTzS2z4JN9uz3GcNG8aMRz7MC/uSyJDQ6uiBptyaYy7auXx9"
                            + "iqTQH5I1uv991+wF4B45Nkdi0GGpzb9pImZWu1Gig42hmJDocy4B3Ls8j+Ei"
                            + "2gWUcm3Vek0juM8X7iSv0SKmRGZr74QIU2RlUbX9Aw5fmjLZmYs57yxztO30"
                            + "Hr3Zmq1pf1ZhLmc7dSubvPWhtmpb25pfOhMdk9YY+kqotPGE6DfuqFmCcif5"
                            + "wtkU/Y4yK+VMDNL4Ow9PX0xeBjedmn76ZhO4mOZeQJXZBzWvPGBvFA9Uq1PY"
                            + "qWuPI8PAS8CtRnPeZinrM6GvHE4bnGHNyQy5oCDiLbBoeywS9n53/2TFZsX6"
                            + "2dAGnGHbybvf3fkRJ1MrUpJ0cTNuIna9vEkc5OoOFUqXAI826O1xd15XMT7D"
                            + "q+WSbWl2f5m20gQrpOxzZKY9tWTuvJKGC7Ps/JVcRvHMZbpRUxdBdHmfBSLh"
                            + "b0anx9+Ptuby4HawtTXXKmMA/Z4pDVIMNfeQw6UMDV8b9x9tp4WJFhnjojQW"
                            + "iLdzS7tG5vP14nh/FznPkaAYOFf2KEMcJRgf76VVtcHanJ+sGlyzIRQgy2t/"
                            + "mEQmxm9ChT4uRHtf/qD0zHWucjFx5MwjnO7YSNuFtrceZpcdxD0aQiRF7HzA"
                            + "8mqSdOQal84Vbw818fQ0/rCxed1juDP8T94EPvi8QHfGmSzLmzZ9mc/lsuJF"
                            + "g/aOoy3E05Pz+qRw+0PbzGIPpwdVRYPZn8uvfZ2rnDYSqcsWem8V1f3XfeE1"
                            + "O7NQswMVZb/uPE7ut7n4er+rSWylM264WCrcLFz5dvXtbx12RmO+nCHR3TSu"
                            + "+stdeRbUio85C3HR/G3RJe5LJPwHWUNHc0febvuZZiuVR/Tl4eba5uaa1z5K"
                            + "xNiaMJ6gOlnqS72qVywjtBI4FBxAdkwbujdLJ2YGpaXitHrybyK5yatzxXzZ"
                            + "bydSQkJGbLTrWMo/83DY7dFUvwl+ibTQizoTbLKEGi3KFhiT2obQeI0UeJ/B"
                            + "Fs+PhVgqvLTA9PMZxesYjO/qb8hhhAv8o4YDB6JNll5i7VXEMWR0I0jUuAFN"
                            + "fpMgqZd4F06r/Rr5adztvdFBTCdTLrce9cgj7Yi0ShY/xTKEeIszQ5io56e8"
                            + "T67IoJcExE2z0W6Sm3b4JyPpIksfVwpgUgNfmF3bedb1Bd8fbJUehrq3HlVc"
                            + "qzAXqnDmN9K6FOtIUxL4BkqGO0cka5iNXUY+SuU/lZWV5xewLm87uMgNvmRo"
                            + "IZZHewaPsKNIusctHdX73no32pdL4KYkvLbe5/BBBdidU6Ywhm/51elOv6UX"
                            + "Ek6YwLB1p186AzYWHmEBAgCt7P9OZ2T+nc5g8VgC2s+bYIIlEf9HW9KQbiGQ"
                            + "IaGwJ8+TCnxnOk+5KB0HpnRS4m/lmz/FJLOfrq2E82tAqOGVoVJmkvGZ9M4T"
                            + "WA+3HK69YUqWPG/9RYEzCNPsIp7ssQsHIwoHcquWD5oD9Y+6ydpeae6dqNvN"
                            + "3G10rVt0/CjxY29TAbDWko6m+V5qa0OrTOuxJz5+zteu8sZdJU9fOQKkDtbX"
                            + "UV5WKr3QF5CAiPbRjJBFlBggulB8E3GNwb0wn0MBwHHzmBbdK9oLqlpiLzUz"
                            + "h3wQ4aE2xVqF14++W9cDZqzXPiApRxVJMo5yijVLvBxkhJMl9Ne5Hw6t5ymG"
                            + "xWVHrVuC+t2CHI9limUFM1py6E2YDmDubFEcSSpJtcxUcn6kMA3Mj4OclxsE"
                            + "8+ESdo7w4YwWXPNbln0Hk1NeTVCpRXEfy6y6v1KTtJ1HCr/Jw2vUq6/rXhtn"
                            + "xCV1GULRdglUOakMQ4GTUuW0C25G4z6bX8JIFRc9jtRsLEVZPWV9zT4BJmpk"
                            + "7RjOP06Dqy8uuDjHJnIM1mx9MXeSG5MzNr7WMzT00VZf0+8tJdqZErljHRQS"
                            + "ez1AX5ruoMroLOs16jGKe5j2MTdPXGLnuvs5jx4j1/M6Uoo+cubMq66x+sNa"
                            + "bsaoY81Vy/iebigZsyD3/kVCKibfzEyUiL+KLJPlnqGvpPpNKCa+9fU4W+Cf"
                            + "xdmXng/8BrnNj5yFqqbttgC/DB23nJ+MrbN4NWYslLH2LCHY+0avc93Jav2c"
                            + "Y7aUd6RuWEfvKdEsR5vY/Ae9juFrsa+UOmQsKspBFI2ex1bLX+2utfnzW2fN"
                            + "kquCaZ3C8wOdd8kiS7zEoKf2wlCM0In2vlaLKjG51KfT/Rx63RJx1osEF+G+"
                            + "quLg11fBkCtCWcrnKNOCXuTP24TldzEBXglk8uCoBYqraBMGldt9gBfg2s5D"
                            + "CeUnoJJ4a8OiNaWuvl6x4TiiMHMSpLASxPpkOQj0ZCWIRWdwg7h/yYDTUnOt"
                            + "416C2gYlgYmbM4YP5L0SZABvjAn0b7y96x96y8mdkZbgNKumeYFm0EtN2GIg"
                            + "O7w3oOEVYv6Fs7vuxS82nAc32EJzy8JKZhON1YZxq2cLuCZO3OAAibB+E0to"
                            + "IPf7hSbo+AqQNAv9jQuzopxaxJoHKm+ZfEJiJUZzVeyHK2PegZ+iw1FimibI"
                            + "yvk4bcVybqWmIdSDg5F7GSnCexvXyuqrrdUaNm0HqvmT2lRapJIN7o9L7LcH"
                            + "2LDctq0KCPEPvbuGhHNMctyrf85kKVNPsWqqzijHkg6Bh5aSv3YjBnRHLydD"
                            + "Rh8QzCankPGryBtrFfaEnsOcafkRbJMCpXw5BMQwEBkijbJxIWabuoJQr89o"
                            + "SAda1LnQy/3qG059kN8dN3+krvFyrrbI1HjyeMRMpkXduQMLFARZoW5Ntc1T"
                            + "PNuvuSa3ojvlKLsrdc/jSewQSNJSs8Z6wvI6Xl7mdLy4dlMxpchEyVrSasrR"
                            + "dr7Ltg3RdQ7mVoCdtOOhJ0NSsOP5tOKHyyXX6tYRATsu656KnGGbHpSU96P7"
                            + "DbSF0q4gpnEH3/ZVduDlSS7milSzltjr+ial6WdJBSkqZY3YzYBHkvHjQS8T"
                            + "t9QiPsCy+h7mBUz1kSg+C+fDvBZR65NjNzlmJNngtVlm/uFsNbnRaj+g6m+0"
                            + "j8d+9jneHA41ARipNHis/IgrnE0fEPeyPYZ0/Yyl+j4FrEbwjC1wnpIGvJuV"
                            + "Dx1kbRxn07p4EdvYAFSfkrDB5QQBPIQKLioft7CFO0TXDQ/KoMm42aPcfNY5"
                            + "POkDTrkP34iv00Xo8j7g6b/CX0iq8L6IYy/yc8SzXM1wKIUsscSAJR0FoW6+"
                            + "QEjlHd2xVeWRT2giloYdQDnzFiw0pXrkivOGM8Qgqp/ook0BE4spe4/18I8P"
                            + "1zFcl65dSzxkqG9TXHzsnRyV59M8UWL1vVkFZq4+W2e5/X4kR7tnVcTW3X5S"
                            + "Q3+dN/Z1IIaKuGhjw0atdrxoqQKFRd422mQ3Ooq5mMjGXJ+OUBEdeFo/FTSn"
                            + "JzOEOqMo8Xni2S1VadyVbEPXMpWhk0FlgaUd/swCLHeQMkHQsa6XO8xO6EmX"
                            + "UB++lSGfNsnznFOg4IR2aKC01zNCpN0oNQYy1L2Qt/gkVChxKn8zp8byzsp1"
                            + "HVum0y0S2kutUid0+nBFvrzn/K7OqCBaLx2C9dbjdy29oxp2UL2W53a9k+uC"
                            + "W8rXSO+8aqUUBNmMG7pSdAxJCifgP2D2bTk/rvHEb40Z+Kc9ebFVfklZ1qqw"
                            + "xkpd3InAWOjSVUPc+cBzV36iKqYczoCMvXb6+u4D46vMjzin9SPjeum3Mr/1"
                            + "A7Q7U/bF1fL75JDHtDsCC2/bapvO9JVljd9wJd9fzVujNHf4l36Kex4Q93ly"
                            + "jYL/LrZqKn/g22T8PeNeh0Mfm60Qz9dVz9t3uLZTSyq5xoVbtJ7r7zXitzU0"
                            + "5etXn07cDS7e8hJZgU6IlexylLWBP3l7/ND51X8DjgjtnmYDANB8v/ovE7Mg"
                            + "4E+f8X8O5JdJ+TP+sCx/h/5uHwT/gGn/C8Pye4Vf7f/3wUDtjwoN/73p+DvJ"
                            + "77+X+YNEivU/mjjMTVgP/YIBfyaUCQDI4/61+we9NCl69A0AAA==")
            ));
    }

    public void testSkipProvidedLibraries() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=187191
        //noinspection all // Sample code
        assertEquals("No warnings.",

                lintProject(
                        manifest().minSdk(14),
                        mLayout,
                        mThemes,
                        mThemes2,
                        mUnsupported
                ));
    }

    @Override
    protected TestLintClient createClient() {
        if ("testSkipProvidedLibraries".equals(getName())) {
            // Set up a mock project model for the resource configuration test(s)
            // where we provide a subset of densities to be included
            return new ToolsBaseTestLintClient() {
                @NonNull
                @Override
                protected Project createProject(@NonNull File dir, @NonNull File referenceDir) {
                    return new Project(this, dir, referenceDir) {
                        @NonNull
                        @Override
                        public List<File> getJavaLibraries(boolean includeProvided) {
                            if (!includeProvided) {
                                return Collections.emptyList();
                            }
                            return super.getJavaLibraries(true);
                        }
                    };
                }
            };
        }

        return super.createClient();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mLayout = xml("res/layout/layout.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"fill_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <!-- Requires API 5 -->\n"
            + "\n"
            + "    <QuickContactBadge\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\" />\n"
            + "\n"
            + "    <!-- Requires API 11 -->\n"
            + "\n"
            + "    <CalendarView\n"
            + "        android:layout_width=\"fill_parent\"\n"
            + "        android:layout_height=\"fill_parent\" />\n"
            + "\n"
            + "    <!-- Requires API 14 -->\n"
            + "\n"
            + "    <GridLayout\n"
            + "        foo=\"@android:attr/actionBarSplitStyle\"\n"
            + "        bar=\"@android:color/holo_red_light\"\n"
            + "        android:layout_width=\"fill_parent\"\n"
            + "        android:layout_height=\"fill_parent\" >\n"
            + "\n"
            + "        <Button\n"
            + "            android:layout_width=\"fill_parent\"\n"
            + "            android:layout_height=\"fill_parent\" />\n"
            + "    </GridLayout>\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mThemes = xml("res/values/themes.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <style name=\"Theme\" parent=\"android:Theme\"/>\n"
            + "\n"
            + "    <style name=\"Theme.Test\" parent=\"android:style/Theme.Light\">\n"
            + "        <item name=\"android:windowNoTitle\">true</item>\n"
            + "        <item name=\"android:windowContentOverlay\">@null</item>\n"
            + "        <!-- Requires API 14 -->\n"
            + "        <item name=\"android:windowBackground\">  @android:color/holo_red_light </item>\n"
            + "    </style>\n"
            + "\n"
            + "    <style name=\"Theme.Test.Transparent\">\n"
            + "        <item name=\"android:windowBackground\">@android:color/transparent</item>\n"
            + "    </style>\n"
            + "\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mThemes2 = xml("res/color/colors.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <style name=\"Theme\" parent=\"android:Theme\"/>\n"
            + "\n"
            + "    <style name=\"Theme.Test\" parent=\"android:style/Theme.Light\">\n"
            + "        <item name=\"android:windowNoTitle\">true</item>\n"
            + "        <item name=\"android:windowContentOverlay\">@null</item>\n"
            + "        <!-- Requires API 14 -->\n"
            + "        <item name=\"android:windowBackground\">  @android:color/holo_red_light </item>\n"
            + "    </style>\n"
            + "\n"
            + "    <style name=\"Theme.Test.Transparent\">\n"
            + "        <item name=\"android:windowBackground\">@android:color/transparent</item>\n"
            + "    </style>\n"
            + "\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mUnsupported = base64gzip("libs/unsupported.jar", ""
            + "H4sIAAAAAAAAAAvwZmYRYeAAwmMzkxwZkAAnAwuDr2uIo66nn5v+v1MMDMwM"
            + "Ad7sHCApJqiSAJyaRYAYrtnX0c/TzTU4RM/X7bPvmdM+3rp6F3m9dbXOnTm/"
            + "OcjgivGDp0V6Xr46nr4XS1excM54KXlEerZ2hoWYyJMlWhXPVF9nfir6WMQI"
            + "tt1tYdofG6DZNlDbucC27UWznRWIS1KLS/RxK+GEKSnITtdH+GM6mjJpZGU+"
            + "mUlFiUWVzjmJxcV6ySAyNyjaX9hR5N+y7bnXs/QDHC5yPVq69ITD0exefpMZ"
            + "GWEmKzOfFG67O+Xg3chNc7n+Kv/jr93Q6fuH8Z/G45u5jpk32i2Nn8/5qZf+"
            + "7fv8+fsZrgSaCyRrZC5f//Pjc7ntS2Q7Em6UuO7PVzg4wV4onqW89dXH798m"
            + "Xr7Is3J6kOffLsW4ldskhXz3v57RoTzZVUizbY7q1M32H3LUf2jkXE/UiKpz"
            + "35EreOKDja/al4VvjHWipk8ylzC6d2FuCs8TyWdOqsv31Ct5nr59t/HaPqOJ"
            + "zmrNllN4zsQL3Jb6tvVx6sYGV6FX/B7lJ7tOXXouz7SyxJu974OU2rrkmwd6"
            + "NQ/6WHbP3nE0QaZdM1zQ4+isuR6Lb5kV/H6zz+LiHs2mdaptR7IW9fQ0WvN8"
            + "Drwq/GvC+1P3pJfOnSe8pHD6wTvr7G9V/nnycvPzaLWwQnuZx82SakHO26Qf"
            + "7gkuS/l75vwZl4y8Yyufv1vZeHyD2dsFLNuXvipaOGV967R9j+ar+V6ZX6S8"
            + "8jnzrhcNUo+2vTHUiZhuuWDTzU/sjscrdQ+H6/753zH7Ie8mFwGO/RJvX4gv"
            + "vLpAePkJDbXr7h713afU1q7UmHlMNGrzZLaucE2jGOv9f6YqTBYxP3ZCtqfj"
            + "m3XXVvmIpPcZmx1nG56aEn9TPvnrgh1mh/aKd9bLPOU43BNR1BKn8EfVKX5h"
            + "MO/Pjur0Jvuny6Y7sYYm6SdIvr4iuvidzlX5SZOknpqfDGh6FHZk019xUFL9"
            + "+WuOhgQwpyQzg5IqI5MIA2pWYYKnRBYGVICS69C1IucAERRttjjyHMgELgbc"
            + "OQUBdiPyDW4tnCha7qHmI4RbQTkJOQikUbSxMuLNVwHerGwQ57EyGACVpjGD"
            + "eADV4J9drAQAAA==");
}
