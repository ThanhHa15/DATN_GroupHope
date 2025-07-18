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
function updateCartCountUI(count) {
  const el = document.querySelector(".cart-count");
  if (el) el.textContent = count;

  // Cập nhật cả tiêu đề giỏ hàng nếu có
  const cartTitle = document.querySelector(".cart-title");
  if (cartTitle) {
    cartTitle.textContent = `Giỏ hàng (${count})`;
  }
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
      let totalQuantity = 0;

      items.forEach((item) => {
        const imageUrl = item.image
          ? `/images/${item.image}`
          : "https://via.placeholder.com/40x40?text=No+Image";
        const itemTotal = item.price * item.quantity;
        total += itemTotal;
        totalQuantity += item.quantity;

        container.innerHTML += `
          <div class="flex gap-3 border-b pb-3 mb-3" data-variant-id="${item.variantId
          }">
            <img src="${imageUrl}" class="w-15 h-20 rounded" />
            <div class="flex-1">
              <div class="flex justify-between">
                <p class="font-semibold text-sm text-black">
                  ${item.productName} ${item.storage || ""} - ${item.color || ""
          }
                </p>
                <button
                  onclick="removeFromCart(${item.variantId})"
                  class="text-gray-400 hover:text-red-600 focus:outline-none transition-colors"
                  type="button"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
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
          <span class="text-black">Tổng (${totalQuantity} sản phẩm):</span>
          <span class="text-red-600">${formatCurrency(total)}</span>
        </div>
        <a href="/checkout" class="block w-full text-center bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">
          Thanh toán
        </a>
      `;

      updateCartCount(totalQuantity);
    });
}

function updateCartCountUI(count) {
  // Đảm bảo count luôn là số, mặc định 0 nếu undefined/null
  const displayCount = Number(count) || 0;

  // Cập nhật tất cả các phần tử .cart-count
  document.querySelectorAll(".cart-count").forEach((el) => {
    el.textContent = displayCount;
  });

  // Cập nhật các phần tử .cart-title
  document.querySelectorAll(".cart-title").forEach((cartTitle) => {
    cartTitle.innerHTML = `Giỏ hàng (<span class="cart-count">${displayCount}</span>)`;
  });
}

// Cập nhật hàm updateCartCount để đảm bảo luôn truyền số
function updateCartCount(count) {
  // Nếu count không được truyền vào, thì gọi API để lấy
  if (count === undefined) {
    fetch("/api/cart/count")
      .then((response) => response.json())
      .then((data) => {
        // Truyền data.count hoặc 0 nếu không có
        updateCartCountUI(data.count || 0);
      })
      .catch(() => {
        // Nếu có lỗi khi gọi API, vẫn hiển thị 0
        updateCartCountUI(0);
      });
  } else {
    // Truyền count hoặc 0 nếu count là undefined/null
    updateCartCountUI(count || 0);
  }
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
  Swal.fire({
    title: 'Xác nhận xóa?',
    text: 'Bạn có chắc muốn xóa sản phẩm này không?',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#d33',
    cancelButtonColor: '#3085d6',
    confirmButtonText: 'Xóa',
    cancelButtonText: 'Hủy'
  }).then((result) => {
    if (result.isConfirmed) {
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
          // ✅ Xóa sản phẩm khỏi DOM
          document.getElementById(`cart-item-${variantId}`)?.remove();
        })
        .catch((err) => {
          Swal.fire({
            icon: 'error',
            title: 'Lỗi',
            text: err.message
          });
        });
    }
  });
}



function updateQuantity(variantId, newQty) {
  if (newQty < 1) {
    // Nếu số lượng nhỏ hơn 1 thì xóa luôn, KHÔNG hỏi xác nhận
    removeFromCart(variantId);
    return;
  }

  // Nếu số lượng hợp lệ, thì cập nhật
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

