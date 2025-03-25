// Ejecutar con node generate_inserts.js
const fs = require('fs');

// Categorías
const categories = ['Electronics', 'Sports', 'Nutrition', 'Home', 'Furniture', 
                   'Books', 'Kitchen', 'Outdoors', 'Clothing', 'Beauty', 
                   'Toys', 'Office', 'Garden', 'Automotive', 'Pet Supplies'];

// Diccionarios de productos por categoría
const productsByCategory = {
    'Electronics': ['Smartphone', 'Laptop', 'Tablet', 'Headphones', 'Speaker', 'TV', 'Monitor', 
                   'Keyboard', 'Mouse', 'Router', 'Hard Drive', 'Camera', 'Drone', 'Game Console'],
    'Sports': ['Running Shoes', 'Yoga Mat', 'Dumbbell Set', 'Exercise Bike', 'Basketball', 
              'Tennis Racket', 'Golf Clubs', 'Swimsuit', 'Treadmill', 'Fitness Tracker'],
    'Nutrition': ['Protein Powder', 'Vitamins', 'Energy Bars', 'Pre-Workout', 'Amino Acids',
                 'Weight Gainer', 'Green Tea Extract', 'Fish Oil', 'Creatine', 'Meal Replacement'],
    'Home': ['Coffee Maker', 'Vacuum Cleaner', 'Air Purifier', 'Humidifier', 'Curtains', 
            'Bedding Set', 'Throw Pillows', 'Wall Clock', 'Picture Frame', 'Light Fixture'],
    'Furniture': ['Desk Chair', 'Bookshelf', 'Sofa', 'Dining Table', 'Coffee Table', 
                 'Bed Frame', 'Wardrobe', 'Side Table', 'TV Stand', 'Dresser'],
    'Books': ['Novel', 'Cookbook', 'Biography', 'Self-Help Book', 'Children\'s Book', 
             'Textbook', 'Art Book', 'Comic Book', 'Travel Guide', 'Dictionary'],
    'Kitchen': ['Cookware Set', 'Knife Set', 'Blender', 'Food Processor', 'Toaster',
               'Microwave', 'Dish Set', 'Utensil Set', 'Cutting Board', 'Food Storage'],
    'Outdoors': ['Hiking Backpack', 'Tent', 'Sleeping Bag', 'Camping Stove', 'Fishing Rod',
                'Binoculars', 'Outdoor Chair', 'Cooler', 'Grill', 'Hammock'],
    'Clothing': ['T-Shirt', 'Jeans', 'Dress', 'Jacket', 'Sweater', 'Socks', 'Hat', 
                'Gloves', 'Scarf', 'Shoes', 'Swimwear', 'Underwear', 'Coat'],
    'Beauty': ['Shampoo', 'Conditioner', 'Face Cream', 'Mascara', 'Lipstick', 
              'Perfume', 'Nail Polish', 'Hair Dryer', 'Straightener', 'Face Mask'],
    'Toys': ['Action Figure', 'Board Game', 'Building Blocks', 'Doll', 'Puzzle', 
            'Remote Control Car', 'Stuffed Animal', 'Educational Toy', 'Art Set', 'Bicycle'],
    'Office': ['Notebook', 'Pen Set', 'Desk Organizer', 'Calculator', 'Stapler', 
              'Paper Shredder', 'Whiteboard', 'Filing Cabinet', 'Desk Lamp', 'Printer'],
    'Garden': ['Plant Pot', 'Gardening Tools', 'Garden Hose', 'Bird Feeder', 'Lawn Mower', 
              'Outdoor Lights', 'Seeds', 'Fertilizer', 'Weed Killer', 'Compost Bin'],
    'Automotive': ['Car Cover', 'Floor Mats', 'Air Freshener', 'Tool Kit', 'Jump Starter', 
                  'Phone Mount', 'Dash Cam', 'Seat Covers', 'Oil Filter', 'Wiper Blades'],
    'Pet Supplies': ['Dog Food', 'Cat Litter', 'Pet Bed', 'Pet Toys', 'Aquarium', 
                    'Bird Cage', 'Pet Carrier', 'Feeding Bowl', 'Collar', 'Leash']
};

// Adjetivos y modificadores para los nombres de productos
const adjectives = ['Premium', 'Deluxe', 'Professional', 'Advanced', 'Classic', 'Essential', 'Luxury',
                  'Ultra', 'Super', 'Elite', 'Basic', 'Standard', 'Compact', 'XL', 'Mini'];

// Marcas ficticias
const brands = ['TechPro', 'EcoLife', 'NatureFit', 'PowerMax', 'HomeStar', 'ComfortZone', 'EliteGear',
              'SmartChoice', 'PrimePick', 'ValuePlus', 'GoldenEdge', 'SilverLine', 'BlueSky', 'RedRock',
              'GreenLeaf', 'BlackDiamond', 'WhiteSwan', 'OrangeSun', 'PurpleHaze', 'YellowStone'];

// Rangos de precios por categoría
const priceRanges = {
    'Electronics': [49.99, 1999.99],
    'Sports': [19.99, 599.99],
    'Nutrition': [9.99, 79.99],
    'Home': [29.99, 399.99],
    'Furniture': [79.99, 1499.99],
    'Books': [7.99, 99.99],
    'Kitchen': [19.99, 399.99],
    'Outdoors': [29.99, 699.99],
    'Clothing': [14.99, 199.99],
    'Beauty': [5.99, 149.99],
    'Toys': [9.99, 199.99],
    'Office': [4.99, 299.99],
    'Garden': [14.99, 499.99],
    'Automotive': [9.99, 299.99],
    'Pet Supplies': [7.99, 199.99]
};

// Función para generar un número aleatorio entre min y max
function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

// Función para generar un número decimal aleatorio entre min y max con 2 decimales
function randomPrice(min, max) {
    return (Math.random() * (max - min) + min).toFixed(2);
}

// Función para obtener un elemento aleatorio de un array
function randomElement(array) {
    return array[Math.floor(Math.random() * array.length)];
}

// Función para generar un nombre de producto
function generateProductName() {
    const category = randomElement(categories);
    const productType = randomElement(productsByCategory[category]);
    
    // 70% de probabilidad de incluir una marca
    const includeBrand = Math.random() < 0.7;
    const brand = includeBrand ? randomElement(brands) : "";
    
    // 60% de probabilidad de incluir un adjetivo
    const includeAdj = Math.random() < 0.6;
    const adj = includeAdj ? randomElement(adjectives) : "";
    
    // 40% de probabilidad de incluir un modelo/número
    const includeModel = Math.random() < 0.4;
    const model = includeModel ? randomElement(["Pro", "Lite", "Max", "Plus", "S", "X", "Z"]) + randomInt(1, 9) : "";
    
    const parts = [brand, adj, productType, model].filter(part => part !== "");
    return {
        name: parts.join(" "),
        category: category
    };
}

// Generar los 10,000 INSERTs
let insertStatements = '';
for (let i = 0; i < 100000; i++) {
    const product = generateProductName();
    const category = product.category;
    const name = product.name.replace(/'/g, "''"); // Escapar comillas simples para SQL
    
    // Generar precio dentro del rango apropiado para la categoría
    const [minPrice, maxPrice] = priceRanges[category];
    const price = randomPrice(minPrice, maxPrice);
    
    // Generar cantidad de stock
    const stock = randomInt(5, 500);
    
    // Crear el INSERT
    const insert = `INSERT INTO products (name, price, category, stock) VALUES ('${name}', ${price}, '${category}', ${stock});\n`;
    insertStatements += insert;
}

// Guardar los INSERTs en un archivo
fs.writeFileSync('data.sql', insertStatements);
console.log("Se han generado 100,000 INSERTs en el archivo 'data.sql'");