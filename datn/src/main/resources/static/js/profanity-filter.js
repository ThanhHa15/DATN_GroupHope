const profanityList = [
    // Các từ tục, thô tục
    'dm', 'đm', 'dmm', 'đmm', 'dmmm', 'đmmm', 'dcm', 'đcm', 'dcmm', 'đcmm',
    'cc', 'cl', 'clgt', 'cmnr', 'cmn', 'đệt',
    'dit', 'đit', 'địt', 'djt', 'đjt',
    'vcl', 'vl', 'vleu',
    // Các từ xúc phạm
    'ngu', 'óc chó', 'súc vật', 'chó', 'lồn', 'loz', 'lozz',
    // Thêm các từ khác vào đây
];

// Hàm kiểm tra nội dung có chứa từ ngữ không phù hợp
function containsProfanity(text) {
    if (!text) return false;

    // Chuyển text về chữ thường và bỏ dấu để dễ so sánh
    const normalizedText = text.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

    // Kiểm tra từng từ trong danh sách
    for (const word of profanityList) {
        const normalizedWord = word.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
        if (normalizedText.includes(normalizedWord)) {
            return true;
        }
    }

    return false;
}

// Hàm lấy các từ ngữ không phù hợp trong văn bản
function findProfanityWords(text) {
    if (!text) return [];

    const foundWords = [];
    const normalizedText = text.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

    for (const word of profanityList) {
        const normalizedWord = word.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
        if (normalizedText.includes(normalizedWord)) {
            foundWords.push(word);
        }
    }

    return foundWords;
}