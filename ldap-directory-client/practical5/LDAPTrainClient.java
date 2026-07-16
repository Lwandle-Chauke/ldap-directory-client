import java.io.*;
import java.net.*;

public class LDAPTrainClient {

    private static final String LDAP_HOST = "localhost";
    private static final int LDAP_PORT = 389;
    private static final String BASE_DN = "ou=Trains,dc=assets,dc=local";

    public static void main(String[] args) {
        try {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter train name: ");
            String trainName = console.readLine().trim();
            if (trainName.isEmpty()) {
                System.err.println("No train name entered.");
                return;
            }

            try (Socket socket = new Socket(LDAP_HOST, LDAP_PORT);
                 OutputStream rawOut = socket.getOutputStream();
                 InputStream rawIn = socket.getInputStream()) {

                // 1. Send BindRequest
                byte[] bindReq = buildBindRequest();
                rawOut.write(bindReq);
                rawOut.flush();

                // 2. Read BindResponse
                byte[] bindResp = readLDAPMessage(rawIn);
                if (!checkBindSuccess(bindResp)) {
                    System.err.println("Bind failed.");
                    return;
                }
                System.out.println("Bind successful.");

                // 3. Send SearchRequest
                byte[] searchReq = buildSearchRequest(trainName);
                rawOut.write(searchReq);
                rawOut.flush();

                // 4. Read and parse SearchResponse
                String speed = readSearchResponses(rawIn, trainName);
                if (speed != null) {
                    System.out.println("┌─────────────────────────────┐");
                    System.out.println("│ Train:     " + padRight(trainName, 17) + "│");
                    System.out.println("│ Max Speed: " + padRight(speed + " km/h", 17) + "│");
                    System.out.println("└─────────────────────────────┘");
                } else {
                    System.out.println("Train '" + trainName + "' not found.");
                }

                // 5. Send UnbindRequest (RFC 4511 Section 4.3 - good protocol behaviour)
                rawOut.write(buildUnbindRequest());
                rawOut.flush();
                System.out.println("Unbound from server.");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────
    // BUILD BIND REQUEST (anonymous, version 3)
    // RFC 4511 Section 4.2
    // ─────────────────────────────────────────────
    private static byte[] buildBindRequest() throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        // version INTEGER (3)
        body.write(new byte[]{0x02, 0x01, 0x03});
        // name OCTET STRING ("")
        body.write(new byte[]{0x04, 0x00});
        // authentication: simple [0] ("")
        body.write(new byte[]{(byte)0x80, 0x00});

        // Wrap in BindRequest tag 0x60
        byte[] bindRequest = tlv(0x60, body.toByteArray());

        // LDAPMessage: SEQUENCE { messageID=1, bindRequest }
        ByteArrayOutputStream msg = new ByteArrayOutputStream();
        msg.write(new byte[]{0x02, 0x01, 0x01}); // messageID = 1
        msg.write(bindRequest);
        return tlv(0x30, msg.toByteArray());
    }

    // ─────────────────────────────────────────────
    // BUILD SEARCH REQUEST
    // RFC 4511 Section 4.5.1
    // ─────────────────────────────────────────────
    private static byte[] buildSearchRequest(String trainName) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        // baseObject: BASE_DN
        body.write(octetString(BASE_DN));

        // scope: wholeSubtree (2)
        body.write(new byte[]{0x0A, 0x01, 0x02});

        // derefAliases: neverDerefAliases (0)
        body.write(new byte[]{0x0A, 0x01, 0x00});

        // sizeLimit: 0 (no limit)
        body.write(new byte[]{0x02, 0x01, 0x00});

        // timeLimit: 0 (no limit)
        body.write(new byte[]{0x02, 0x01, 0x00});

        // typesOnly: FALSE
        body.write(new byte[]{0x01, 0x01, 0x00});

        // filter: equalityMatch (cn=trainName)
        // Tag 0xA3 = [3] IMPLICIT — equalityMatch
        // Content = AttributeValueAssertion: OCTET STRING attr, OCTET STRING value
        // NO extra SEQUENCE wrapper — this was your bug
        ByteArrayOutputStream filterBody = new ByteArrayOutputStream();
        filterBody.write(octetString("cn"));
        filterBody.write(octetString(trainName));
        body.write(tlv(0xA3, filterBody.toByteArray()));

        // attributes: SEQUENCE OF LDAPString — request only "description"
        byte[] attrList = tlv(0x30, octetString("description"));
        body.write(attrList);

        // Wrap in SearchRequest tag 0x63
        byte[] searchRequest = tlv(0x63, body.toByteArray());

        // LDAPMessage SEQUENCE { messageID=2, searchRequest }
        ByteArrayOutputStream msg = new ByteArrayOutputStream();
        msg.write(new byte[]{0x02, 0x01, 0x02}); // messageID = 2
        msg.write(searchRequest);
        return tlv(0x30, msg.toByteArray());
    }

    // ─────────────────────────────────────────────
    // BUILD UNBIND REQUEST
    // RFC 4511 Section 4.3
    // ─────────────────────────────────────────────
    private static byte[] buildUnbindRequest() throws IOException {
        // UnbindRequest is NULL (tag 0x42, length 0)
        ByteArrayOutputStream msg = new ByteArrayOutputStream();
        msg.write(new byte[]{0x02, 0x01, 0x03}); // messageID = 3
        msg.write(new byte[]{0x42, 0x00});        // UnbindRequest NULL
        return tlv(0x30, msg.toByteArray());
    }

    // ─────────────────────────────────────────────
    // READ A FULL LDAP MESSAGE FROM SOCKET
    // ─────────────────────────────────────────────
    private static byte[] readLDAPMessage(InputStream in) throws IOException {
        int tag = in.read();
        if (tag == -1) throw new EOFException("Connection closed");

        // Read BER length
        int firstByte = in.read();
        int length;
        int extraLenBytes = 0;
        if ((firstByte & 0x80) == 0) {
            length = firstByte;
        } else {
            extraLenBytes = firstByte & 0x7F;
            length = 0;
            for (int i = 0; i < extraLenBytes; i++) {
                length = (length << 8) | in.read();
            }
        }

        // Read value
        byte[] value = new byte[length];
        int read = 0;
        while (read < length) {
            int r = in.read(value, read, length - read);
            if (r == -1) throw new EOFException("Truncated message");
            read += r;
        }

        // Reassemble full TLV
        ByteArrayOutputStream full = new ByteArrayOutputStream();
        full.write(tag);
        if (extraLenBytes == 0) {
            full.write(firstByte);
        } else {
            full.write(0x80 | extraLenBytes);
            for (int i = extraLenBytes - 1; i >= 0; i--) {
                full.write((length >> (8 * i)) & 0xFF);
            }
        }
        full.write(value);
        return full.toByteArray();
    }

    // ─────────────────────────────────────────────
    // CHECK BIND RESPONSE SUCCESS
    // ─────────────────────────────────────────────
    private static boolean checkBindSuccess(byte[] msg) {
        // Scan for ENUMERATED 0x0A 0x01 0x00 (resultCode = success)
        for (int i = 0; i < msg.length - 2; i++) {
            if ((msg[i] & 0xFF) == 0x0A &&
                (msg[i+1] & 0xFF) == 0x01 &&
                (msg[i+2] & 0xFF) == 0x00) {
                return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────
    // READ SEARCH RESPONSES
    // ─────────────────────────────────────────────
    private static String readSearchResponses(InputStream in, String trainName) throws IOException {
        while (true) {
            byte[] msg = readLDAPMessage(in);

            // Find the inner tag (skip SEQUENCE tag+len, then messageID TLV)
            int offset = 0;
            offset++; // 0x30 outer SEQUENCE tag
            offset += berLenSize(msg, offset); // skip length bytes

            // skip messageID: 0x02 0x01 XX
            offset++; // 0x02
            int idLen = msg[offset++] & 0xFF;
            offset += idLen;

            int innerTag = msg[offset++] & 0xFF;

            if (innerTag == 0x64) {
                // SearchResultEntry — parse it
                String speed = parseSearchResultEntry(msg, offset);
                if (speed != null) return speed;

            } else if (innerTag == 0x65) {
                // SearchResultDone
                offset += berLenSize(msg, offset); // skip length
                offset++; // ENUMERATED tag 0x0A
                offset++; // length 0x01
                int resultCode = msg[offset] & 0xFF;
                if (resultCode != 0) {
                    System.err.println("Search failed, LDAP result code: " + resultCode);
                }
                return null;
            }
        }
    }

    // ─────────────────────────────────────────────
    // PARSE SearchResultEntry — extract description
    // ─────────────────────────────────────────────
    private static String parseSearchResultEntry(byte[] msg, int offset) throws IOException {
        // Skip SearchResultEntry length
        offset += berLenSize(msg, offset);

        // objectName (DN) — OCTET STRING
        offset++; // 0x04 tag
        int dnLen = berLen(msg, offset);
        offset += berLenSize(msg, offset);
        offset += dnLen; // skip DN bytes

        // partialAttributes — SEQUENCE
        offset++; // 0x30 tag
        int attrsLen = berLen(msg, offset);
        offset += berLenSize(msg, offset);
        int attrsEnd = offset + attrsLen;

        while (offset < attrsEnd) {
            // Each attribute: SEQUENCE { type OCTET STRING, vals SET OF OCTET STRING }
            offset++; // 0x30
            int attrLen = berLen(msg, offset);
            offset += berLenSize(msg, offset);
            int attrEnd = offset + attrLen;

            // Attribute type
            offset++; // 0x04
            int typeLen = berLen(msg, offset);
            offset += berLenSize(msg, offset);
            String attrType = new String(msg, offset, typeLen, "UTF-8");
            offset += typeLen;

            // SET of values
            offset++; // 0x31
            int setLen = berLen(msg, offset);
            offset += berLenSize(msg, offset);
            int setEnd = offset + setLen;

            while (offset < setEnd) {
                offset++; // 0x04
                int valLen = berLen(msg, offset);
                offset += berLenSize(msg, offset);
                String value = new String(msg, offset, valLen, "UTF-8");
                offset += valLen;

                if (attrType.equalsIgnoreCase("description")) {
                    return value.trim();
                }
            }
            offset = attrEnd;
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // BER / ASN.1 HELPERS
    // ─────────────────────────────────────────────

    // Wrap content in a TLV with given tag
    private static byte[] tlv(int tag, byte[] content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        // BER length encoding
        if (content.length < 128) {
            out.write(content.length);
        } else if (content.length < 256) {
            out.write(0x81);
            out.write(content.length);
        } else {
            out.write(0x82);
            out.write((content.length >> 8) & 0xFF);
            out.write(content.length & 0xFF);
        }
        out.write(content);
        return out.toByteArray();
    }

    // Encode a UTF-8 string as BER OCTET STRING (tag 0x04)
    private static byte[] octetString(String s) throws IOException {
        byte[] bytes = s.getBytes("UTF-8");
        return tlv(0x04, bytes);
    }

    // Read BER-encoded length value at offset
    private static int berLen(byte[] data, int offset) {
        int first = data[offset] & 0xFF;
        if ((first & 0x80) == 0) return first;
        int numBytes = first & 0x7F;
        int len = 0;
        for (int i = 1; i <= numBytes; i++) {
            len = (len << 8) | (data[offset + i] & 0xFF);
        }
        return len;
    }

    // How many bytes does the BER length field occupy at offset?
    private static int berLenSize(byte[] data, int offset) {
        int first = data[offset] & 0xFF;
        if ((first & 0x80) == 0) return 1;
        return 1 + (first & 0x7F);
    }

    // Pad a string to width for display
    private static String padRight(String s, int width) {
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}
