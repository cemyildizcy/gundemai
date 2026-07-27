import assert from "node:assert/strict";
import test from "node:test";

import { distinctSourceCount } from "../src/source-identity";

test("different URLs from the same publisher count as one source", () => {
  assert.equal(
    distinctSourceCount([
      { name: "NTV Gündem", url: "https://www.ntv.com.tr/turkiye/haber-1" },
      { name: "NTV Gündem", url: "https://www.ntv.com.tr/turkiye/haber-2" }
    ]),
    1
  );
});

test("publisher host normalization ignores www and a trailing dot", () => {
  assert.equal(
    distinctSourceCount([
      { name: "Koin Bülteni", url: "https://koinbulteni.com/haber-1" },
      { name: "Koin Bülteni", url: "https://www.koinbulteni.com./haber-2" }
    ]),
    1
  );
});

test("different Telegram channels count as independent sources", () => {
  assert.equal(
    distinctSourceCount([
      { name: "BPT", url: "https://t.me/bpthaber/101" },
      { name: "Pusholder", url: "https://t.me/pusholder/202" }
    ]),
    2
  );
});
