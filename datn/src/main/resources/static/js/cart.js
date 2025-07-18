function showNotification(message, type = "success") {
  const container = document.getElementById("notification-container");
  if (!container) return;

  const notif = document.createElement("div");
  notif.className = "notification";

  // Tạo phần HTML bên trong với icon nếu là success
  let iconHtml = "";
  if (type === "success") {
    iconHtml = `<img src="/images/sc.png" alt="success" class="w-6 h-6 mr-2 inline-block" />`;
  }

  if (type === "error") {
    notif.style.backgroundColor = "#fee2e2";
    notif.style.color = "#991b1b";
    notif.style.borderLeftColor = "#dc2626";
  }

  notif.innerHTML = iconHtml + message;

  container.appendChild(notif);

  // Hiện ra
  setTimeout(() => {
    notif.classList.add("show");
  }, 50);

  // Tự ẩn
  setTimeout(() => {
    notif.classList.remove("show");
    notif.classList.add("fade-out");
    setTimeout(() => container.removeChild(notif), 500);
  }, 2500);
}

function addToCart(button) {
  const variantId = button.getAttribute("data-variant-id");

  fetch("/api/cart/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: `variantId=${variantId}&quantity=1`,
  })
    .then((response) => {
      if (!response.ok) {
        return response.text().then((text) => {
          throw new Error(text);
        });
      }
      return response.text(); // OK
    })
    .then((data) => {
      showNotification("Đã thêm sản phẩm vào giỏ hàng");

      updateCartCount();
      updateMiniCart();
    })
    .catch((error) => {
      showNotification("❌ Lỗi: " + error.message, "error");
    });
}

function updateCartCount() {
  fetch("/api/cart/count")
    .then((response) => response.json())
    .then((count) => {
      const el = document.querySelector(".cart-count");
      if (el) el.textContent = count;
    });
}

function updateMiniCart() {
  fetch("/api/cart/items")
    .then((response) => {
      if (response.status === 401) {
        console.log("Chưa đăng nhập - không tải giỏ hàng");
        return [];
      }
      return response.json();
    })
    .then((items) => {
      if (!Array.isArray(items)) return;

      const container = document.querySelector(".group .absolute.z-50.p-4");
      if (!container) return;

      container.innerHTML = "";

      if (items.length === 0) {
        container.innerHTML = `
          <div class="flex flex-col items-center justify-center py-6 text-gray-700">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-12 h-12 mb-3 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13l-1.35 2.7a1 1 0 00.9 1.5H19M7 13l.5 2m6.5-2l1 2m-9 4h.01M17 17h.01"/>
            </svg>
            <div class="text-xl font-semibold text-gray-800">Chưa có sản phẩm</div>
            <p class="text-sm text-gray-500 mt-1">Giỏ hàng của bạn đang trống</p>
          </div>

        `;
        return;
      }

      let total = 0;

      items.forEach((item) => {
        const imageUrl = item.image
          ? `/images/${item.image}`
          : "https://via.placeholder.com/60x60?text=No+Image";
        const itemTotal = item.price * item.quantity;
        total += itemTotal;

        container.innerHTML += `
          <div class="flex gap-3 border-b pb-3 mb-3" data-variant-id="${item.variantId
          }">
<img src="${imageUrl}" class="w-12 h-12 object-cover rounded" />
            <div class="flex-1">
              <div class="flex justify-between">
                <p class="font-semibold text-sm text-black">
                  ${item.productName} ${item.storage || ""} - ${item.color || ""
          }
                </p>
                <button
                  onclick="removeFromCart(${item.variantId})"
                  class="text-gray-400 hover:text-red-600 focus:outline-none"
                  type="button"
                >Xóa</button>
              </div>
              
              <div class="flex justify-between items-center mt-2">
                <div class="flex items-center border rounded">
                  <button class="px-2 text-gray-600 hover:bg-gray-100" onclick="updateQuantity(${item.variantId
          }, ${item.quantity - 1})">-</button>
                  <span class="px-2 text-black" id="qty-${item.variantId}">${item.quantity
          }</span>
                  <button class="px-2 text-gray-600 hover:bg-gray-100" onclick="updateQuantity(${item.variantId
          }, ${item.quantity + 1})">+</button>
                </div>
                <div class="text-red-600 font-bold text-right text-sm" id="item-total-${item.variantId
          }">
                  ${formatCurrency(itemTotal)}
                </div>
              </div>
            </div>
          </div>
        `;
      });

      container.innerHTML += `
        <div class="flex justify-between font-semibold text-base mt-4 mb-4">
          <span class="text-black">Tổng tiền:</span>
          <span class="text-red-600">${formatCurrency(total)}</span>
        </div>
        <a href="/checkout" class="block w-full text-center bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">
          Thanh toán
        </a>
      `;
    });
}
function formatCurrency(number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(number);
}

document.addEventListener("DOMContentLoaded", function () {
  updateCartCount();
  updateMiniCart();
});

function removeFromCart(variantId) {
  if (!confirm("Bạn có chắc muốn xóa sản phẩm này không?")) return;

  fetch("/api/cart/remove", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: `variantId=${variantId}`,
  })
    .then((res) => {
      if (!res.ok) throw new Error("Lỗi khi xóa sản phẩm");
      return res.text();
    })
    .then(() => {
      updateMiniCart();
      updateCartCount();
    })
    .catch((err) => alert("❌ " + err.message));
}

function updateQuantity(variantId, newQty) {
  fetch("/api/cart/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: `variantId=${variantId}&quantity=${newQty}`,
  })
    .then((res) => {
      if (!res.ok) throw new Error("Không thể cập nhật số lượng");
      return res.text();
    })
    .then(() => {
      updateMiniCart();
      updateCartCount();
    })
    .catch((err) => alert("❌ " + err.message));
}
